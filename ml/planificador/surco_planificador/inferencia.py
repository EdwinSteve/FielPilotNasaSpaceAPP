"""Servicio en CPU: el bundle exportado recomienda el mejor ciclo productivo para UN lote.

Solo necesita numpy, pyyaml y onnxruntime (sin torch ni GPU). Flujo para una fecha de siembra:

1. El último pronóstico SEAS5 publicado (51 miembros) se interpola al lote y se corrige con los
   parámetros del punto de entrenamiento más cercano (más el ajuste por elevación).
2. Se arman las mismas entradas del entrenamiento (caracteristicas.py) y los priors B0 y Bf con
   el mismo oráculo.
3. La red puntúa todos los cultivos en una pasada y se ordenan por rendimiento esperado.
"""

import json
from dataclasses import dataclass
from datetime import date, timedelta
from pathlib import Path

import numpy as np
import numpy.typing as npt
import onnxruntime as ort

from surco_planificador.caracteristicas import (
    CANALES_ANOMALIA,
    CANALES_PRONOSTICO_DIARIO,
    CLIM_DE_ANOMALIA,
    DIAS_ADELANTE,
    PENTADA,
    PENTADAS_PRONOSTICO,
    Normalizacion,
    a_objetivos,
    a_pentadas,
    transformar_clim,
    transformar_historia,
    vector_contexto,
)
from surco_planificador.catalogo import FichaCultivo, vector_ficha
from surco_planificador.etiquetas import estadisticas_bf
from surco_planificador.exportar import ENTRADAS
from surco_planificador.limpieza.climatologia import INDICE, indice_doy
from surco_planificador.oraculo import CicloClima, Oraculo
from surco_planificador.pronostico import Correccion, aplicar_correccion, derivar_diario

F32 = npt.NDArray[np.float32]
_D = {c: i for i, c in enumerate(CANALES_PRONOSTICO_DIARIO)}


@dataclass(frozen=True)
class Lote:
    """Lo que el backend sabe del lote el día de siembra."""

    lat: float
    lon: float
    elevacion_m: float
    sitio: F32  # [len(SITIO)] como en el entrenamiento (suelo de SoilGrids en el lote)
    historia: F32  # [365, len(CANALES_HISTORIA)] de POWER, termina el día antes de sembrar
    clim: F32  # [366, len(SALIDAS)] climatología servida del lote (todos los años base)
    ph_suelo: float
    taw_por_cultivo: dict[str, float]  # FAO-56 Ec. 82 con la Zr de cada cultivo
    riego: bool = False


@dataclass(frozen=True)
class Recomendacion:
    cultivo: str
    rend_p10: float
    rend_p50: float
    rend_p90: float
    prob_exito: float
    riego_mm_p50: float
    rend_con_riego_p50: float


class Planificador:
    def __init__(self, carpeta: Path, fichas: list[FichaCultivo]) -> None:
        int8 = carpeta / "model_int8.onnx"
        self.sesion = ort.InferenceSession(str(int8 if int8.exists() else carpeta / "model.onnx"),
                                           providers=["CPUExecutionProvider"])  # fmt: skip
        self.norm = Normalizacion.cargar(carpeta / "normalizacion.json")
        meta = json.loads((carpeta / "cultivos.json").read_text(encoding="utf-8"))
        por_id = {f.id: f for f in fichas}
        self.fichas = [por_id[c["id"]] for c in meta["cultivos"]]
        cambiadas = [c["id"] for c in meta["cultivos"]
                     if not np.allclose(vector_ficha(por_id[c["id"]]), c["vector"], atol=1e-4)]  # fmt: skip
        if cambiadas:  # la red usa la ficha nueva; conviene reentrenar pronto
            print(f"aviso: fichas cambiadas desde el entrenamiento: {cambiadas}")
        self.cultivos = self.norm.cultivos.aplicar(np.stack([vector_ficha(f) for f in self.fichas]))
        with np.load(carpeta / "correccion.npz") as c:
            self._cor = {k: c[k] for k in c.files}
        self.oraculos = [Oraculo(f, 0.6) for f in self.fichas]

    def corregir(self, crudo: F32, inicio: date, lote: Lote, gradiente: float = -6.5, bloque: int = 30) -> F32:
        """crudo [M, T, CANALES_CRUDOS] interpolado al lote -> diario corregido [M, T, 6]."""
        d = (self._cor["lat"] - lote.lat) ** 2 + ((self._cor["lon"] - lote.lon) * np.cos(np.radians(lote.lat))) ** 2
        p = int(np.argmin(d))
        cor = Correccion(self._cor["normal"][p], self._cor["cuantiles"][p], bloque)
        if not np.any(cor.normal[inicio.month - 1]):  # meses_inicio de pipeline.yaml no lo incluyó
            raise ValueError(f"el modelo no se entrenó con pronósticos iniciados en el mes {inicio.month}")
        diario = aplicar_correccion(crudo[None].astype(np.float32), np.array([inicio.month]), cor)
        ajuste = gradiente * (lote.elevacion_m - float(self._cor["elevacion"][p])) / 1000
        diario[..., 0:2] += ajuste  # tmax, tmin al piso térmico del lote
        return derivar_diario(diario, lote.lat, lote.elevacion_m, [inicio])[0]

    def recomendar(self, lote: Lote, diario: F32, inicio: date, siembra: date,
                   oni: float, oni_tendencia: float) -> list[Recomendacion]:
        o = (siembra - inicio).days
        if o < 0 or o + PENTADAS_PRONOSTICO * PENTADA > diario.shape[1]:
            raise ValueError("el pronóstico no cubre 175 días desde la siembra: use uno más reciente")
        doy0 = indice_doy(siembra)
        doy = (doy0 + np.arange(DIAS_ADELANTE)) % 366
        clim_ciclo = lote.clim[doy]
        dias = np.arange(o, o + PENTADAS_PRONOSTICO * PENTADA)
        doy_f = np.array([indice_doy(inicio + timedelta(days=int(t))) for t in dias])
        anom = np.stack([diario[:, dias, _D[c]] - lote.clim[doy_f, INDICE[CLIM_DE_ANOMALIA[c]]][None]
                         for c in CANALES_ANOMALIA], -1)  # [M, 175, Ca]  # fmt: skip
        pron = self.norm.anomalia.aplicar(a_pentadas(anom, 1)).transpose(0, 2, 1)[None]
        clim = self.norm.clim.aplicar(transformar_clim(a_pentadas(clim_ciclo, 0))).T[None]
        hist = self.norm.historia.aplicar(transformar_historia(lote.historia)).T[None]
        estatico = self.norm.estatico.aplicar(np.concatenate([
            lote.sitio, np.array(vector_contexto(oni, oni_tendencia, siembra, o), np.float32)]))[None]  # fmt: skip
        prior = self._priors(lote, diario, o, clim_ciclo)[None]
        feed = dict(zip(ENTRADAS, [hist, pron, clim, estatico, self.cultivos, prior], strict=True))
        y = self.sesion.run(None, {k: v.astype(np.float32) for k, v in feed.items()})[0][0]  # [C, 12]
        recs = [Recomendacion(f.id, *map(float, y[c, 0:3]), float(y[c, 3]), float(np.expm1(max(y[c, 5], 0))),
                              float(y[c, 10])) for c, f in enumerate(self.fichas)]  # fmt: skip
        clave = (lambda r: r.rend_con_riego_p50) if lote.riego else (lambda r: r.rend_p50)
        return sorted(recs, key=clave, reverse=True)

    def _priors(self, lote: Lote, diario: F32, o: int, clim_ciclo: F32) -> F32:
        h = lote.historia
        previo = (h[-30:, 4][None], h[-30:, 8][None])  # lluvia y ET0 de los 30 días previos
        clim = {"t": clim_ciclo[:, INDICE["t_media"]], "tmax": clim_ciclo[:, INDICE["tmax_media"]],
                "tmin": clim_ciclo[:, INDICE["tmin_media"]], "lluvia": clim_ciclo[:, INDICE["lluvia_media"]],
                "et0": clim_ciclo[:, INDICE["et0_media"]]}  # fmt: skip
        M = diario.shape[0]
        filas = []
        for f, orc in zip(self.fichas, self.oraculos, strict=True):
            L, taw = f.ciclo_dias, lote.taw_por_cultivo[f.id]
            b0 = orc.simular(CicloClima(*(clim[k][None, :L] for k in ("t", "tmax", "tmin", "lluvia", "et0")),
                                        *previo), taw, lote.ph_suelo)  # fmt: skip
            n = min(L, diario.shape[1] - o)
            base = [np.concatenate([diario[:, o : o + n, _D[c]], np.broadcast_to(clim[k][None, n:L], (M, L - n))], 1)
                    for k, c in (("t", "t2m"), ("tmax", "tmax"), ("tmin", "tmin"), ("lluvia", "lluvia"), ("et0", "et0"))]  # fmt: skip
            bf = orc.simular(CicloClima(*base, np.repeat(previo[0], M, 0), np.repeat(previo[1], M, 0)), taw, lote.ph_suelo)
            ciclo = np.array([L], np.float32)
            filas.append(np.concatenate([a_objetivos(b0, ciclo)[0], estadisticas_bf(bf[None], L)[0]]))
        return np.stack(filas).astype(np.float32)
