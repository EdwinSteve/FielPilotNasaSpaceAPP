"""Entradas del modelo: la única definición, compartida por el entrenamiento y el servicio en CPU.

El entrenamiento aplica estas transformaciones a arreglos completos; el backend, a un lote. Ambos
usan las mismas listas de canales y la misma normalización guardada en el bundle.
"""

import json
import math
from collections.abc import Mapping
from dataclasses import dataclass
from datetime import date
from pathlib import Path

import numpy as np
import numpy.typing as npt

from surco_planificador.fuentes.suelo import PROPIEDADES
from surco_planificador.limpieza.climatologia import SALIDAS

F32 = npt.NDArray[np.float32]

DIAS_HISTORIA = 365
DIAS_ADELANTE = 365  # horizonte del ciclo más largo
PENTADA = 5
PENTADAS_ADELANTE = DIAS_ADELANTE // PENTADA  # 73
PENTADAS_PRONOSTICO = 35  # 175 días: siempre disponibles con cualquier desplazamiento de siembra
PENTADAS_GUARDADAS = 42  # días 1..210 del pronóstico, en péntadas que empiezan el día 1

CANALES_HISTORIA = ("t2m", "tmax", "tmin", "tdew", "lluvia", "hr", "viento", "radiacion",
                    "et0", "gwetroot", "radiacion_estimada")  # fmt: skip
CANALES_CLIM = SALIDAS[:6]  # media, p10 y p90 de temperatura y lluvia
CANALES_PRONOSTICO_DIARIO = ("t2m", "tmax", "tmin", "lluvia", "et0", "hr")  # corregidos
CANALES_ANOMALIA = ("t2m", "tmax", "lluvia", "et0")  # lo que la red ve de cada miembro
CLIM_DE_ANOMALIA = {"t2m": "t_media", "tmax": "tmax_media", "lluvia": "lluvia_media", "et0": "et0_media"}
SITIO = ("lat", "lon", "elevacion_m", "elevacion_menos_celda_m",
         *(f"{p}_0_30" for p in PROPIEDADES), *(f"{p}_30_60" for p in PROPIEDADES))  # fmt: skip
CONTEXTO = ("oni", "oni_tendencia_3m", "doy_sin", "doy_cos", "dias_desde_inicio_pronostico")
ESTATICO = SITIO + CONTEXTO

ETIQUETAS = ("rendimiento", "exito", "riego_mm", "dias_calor", "dias_frio", "rendimiento_riego")
OBJETIVOS = ("rendimiento", "exito", "log1p_riego", "fraccion_calor", "fraccion_frio", "rendimiento_riego")
PRIOR_B0 = tuple(f"b0_{o}" for o in OBJETIVOS)
PRIOR_BF = ("bf_rend_media", "bf_rend_p10", "bf_rend_p90", "bf_exito", "bf_log1p_riego",
            "bf_fraccion_calor", "bf_fraccion_frio", "bf_rend_riego")  # fmt: skip
PRIOR = PRIOR_B0 + PRIOR_BF  # 14
# Salidas de la red (último eje): rendimiento p10/p50/p90, logit de éxito, log1p riego p10/p50/p90,
# fracción de días de calor y de frío, rendimiento con riego p10/p50/p90.
SALIDAS_RED = ("rend_p10", "rend_p50", "rend_p90", "exito_logit", "riego_p10", "riego_p50",
               "riego_p90", "fraccion_calor", "fraccion_frio", "rend_riego_p10",
               "rend_riego_p50", "rend_riego_p90")  # fmt: skip

_LOG_HISTORIA = tuple(i for i, c in enumerate(CANALES_HISTORIA) if c == "lluvia")
_LOG_CLIM = tuple(i for i, c in enumerate(CANALES_CLIM) if c.startswith("lluvia"))


def _mes_atras(anio: int, mes: int, atras: int) -> tuple[int, int]:
    i = anio * 12 + (mes - 1) - atras
    return i // 12, i % 12 + 1


def oni_conocido(oni: Mapping[tuple[int, int], float], fecha: date) -> tuple[float, float]:
    """ONI del trimestre que terminó dos meses antes del mes de siembra, y su cambio en 3 meses.

    CPC publica un trimestre a comienzos del mes siguiente: dos meses atrás siempre está publicado.
    """
    ultimo = oni[_mes_atras(fecha.year, fecha.month, 2)]
    return ultimo, ultimo - oni[_mes_atras(fecha.year, fecha.month, 5)]


def vector_contexto(oni: float, tendencia: float, siembra: date, dias_desde_inicio: int) -> list[float]:
    angulo = 2 * math.pi * (siembra.timetuple().tm_yday - 1) / 365.25
    return [oni, tendencia, math.sin(angulo), math.cos(angulo), dias_desde_inicio / 30.0]


def transformar_historia(x: F32) -> F32:
    out = x.astype(np.float32, copy=True)
    for i in _LOG_HISTORIA:
        out[..., i] = np.log1p(np.maximum(out[..., i], 0.0))
    return out


def transformar_clim(x: F32) -> F32:
    out = x[..., : len(CANALES_CLIM)].astype(np.float32, copy=True)
    for i in _LOG_CLIM:
        out[..., i] = np.log1p(np.maximum(out[..., i], 0.0))
    return out


def a_pentadas(x: F32, eje: int) -> F32:
    """Media por bloques de 5 a lo largo de `eje` (la longitud debe ser múltiplo de 5)."""
    x = np.moveaxis(x, eje, -1)
    x = x.reshape(*x.shape[:-1], -1, PENTADA).mean(-1)
    return np.moveaxis(x, -1, eje).astype(np.float32)


def a_objetivos(etiquetas: F32, ciclos: F32) -> F32:
    """Etiquetas [..., C, 6] -> la escala de la red: log1p del riego y estrés como fracción del ciclo."""
    return np.stack([
        etiquetas[..., 0], etiquetas[..., 1], np.log1p(np.maximum(etiquetas[..., 2], 0.0)),
        etiquetas[..., 3] / ciclos, etiquetas[..., 4] / ciclos, etiquetas[..., 5],
    ], axis=-1).astype(np.float32)  # fmt: skip


@dataclass(frozen=True)
class Estandarizador:
    media: F32
    desv: F32

    @classmethod
    def ajustar(cls, x: F32, eje: tuple[int, ...]) -> "Estandarizador":
        media = np.nanmean(x, axis=eje).astype(np.float32)
        desv = np.nanstd(x, axis=eje).astype(np.float32)
        return cls(media, np.where(desv > 1e-6, desv, 1.0).astype(np.float32))

    def aplicar(self, x: F32) -> F32:
        return ((x - self.media) / self.desv).astype(np.float32)


@dataclass(frozen=True)
class Normalizacion:
    """Estadísticas SOLO de la división de entrenamiento; viajan con el modelo."""

    historia: Estandarizador
    clim: Estandarizador
    anomalia: Estandarizador
    estatico: Estandarizador
    cultivos: Estandarizador

    _NOMBRES = ("historia", "clim", "anomalia", "estatico", "cultivos")

    def guardar(self, ruta: Path) -> None:
        datos: dict[str, object] = {n: {"media": getattr(self, n).media.tolist(),
                                        "desv": getattr(self, n).desv.tolist()} for n in self._NOMBRES}  # fmt: skip
        datos["canales"] = {"historia": list(CANALES_HISTORIA), "clim": list(CANALES_CLIM),
                            "anomalia": list(CANALES_ANOMALIA), "estatico": list(ESTATICO)}  # fmt: skip
        ruta.write_text(json.dumps(datos, indent=1), encoding="utf-8", newline="\n")

    @classmethod
    def cargar(cls, ruta: Path) -> "Normalizacion":
        r = json.loads(ruta.read_text(encoding="utf-8"))
        if r["canales"]["historia"] != list(CANALES_HISTORIA) or r["canales"]["estatico"] != list(ESTATICO):
            raise ValueError("la normalización se guardó para otros canales de entrada")
        return cls(*(Estandarizador(np.array(r[n]["media"], np.float32), np.array(r[n]["desv"], np.float32))
                     for n in cls._NOMBRES))  # fmt: skip
