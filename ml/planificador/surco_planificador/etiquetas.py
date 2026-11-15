"""Etapa `etiquetas`: por punto, la verdad (oráculo sobre POWER), B0 (sobre la climatología) y Bf
(sobre cada miembro corregido del pronóstico), más las anomalías que ve la red.

B0 y Bf son "física primero": la red recibe lo que el oráculo diría con la climatología y con cada
miembro del pronóstico, y aprende a corregirlos con la historia reciente del lote, el ENSO y la
habilidad real del pronóstico en ese lugar y época (que el oráculo no conoce).

Cada punto se guarda en su propio archivo: un corte retoma donde iba.
"""

import logging
import multiprocessing
from collections.abc import Sequence
from concurrent.futures import ProcessPoolExecutor
from dataclasses import dataclass
from datetime import date, timedelta
from pathlib import Path

import numpy as np
import numpy.typing as npt

from surco_planificador.caracteristicas import (
    CANALES_HISTORIA,
    CANALES_PRONOSTICO_DIARIO,
    DIAS_HISTORIA,
    ETIQUETAS,
    PRIOR_BF,
)
from surco_planificador.catalogo import FichaCultivo
from surco_planificador.config import ConfigPipeline
from surco_planificador.datos import DatosLimpios
from surco_planificador.fuentes.suelo import PROPIEDADES, agua_disponible_mm, media_entre
from surco_planificador.limpieza.climatologia import INDICE, indice_doy
from surco_planificador.oraculo import CicloClima, Oraculo
from surco_planificador.pronostico import (
    Correccion,
    ajustar_correccion,
    anomalias_pentadas,
    aplicar_correccion,
    derivar_diario,
    observado_alineado,
    ruta_crudo,
)

log = logging.getLogger(__name__)
F32 = npt.NDArray[np.float32]
_H = {c: i for i, c in enumerate(CANALES_HISTORIA)}
_D = {c: i for i, c in enumerate(CANALES_PRONOSTICO_DIARIO)}


@dataclass(frozen=True)
class DisenoSiembras:
    """Las siembras de entrenamiento: K desplazamientos por cada inicio de pronóstico."""

    inicios: list[date]
    desplazamientos: tuple[int, ...]

    @property
    def n(self) -> int:
        return len(self.inicios) * len(self.desplazamientos)

    def inicio_de(self, s: npt.NDArray[np.int64]) -> npt.NDArray[np.int64]:
        return s // len(self.desplazamientos)

    def desplazamiento_de(self, s: npt.NDArray[np.int64]) -> npt.NDArray[np.int64]:
        return np.asarray(self.desplazamientos)[s % len(self.desplazamientos)]

    def fechas(self) -> list[date]:
        return [i + timedelta(days=o) for i in self.inicios for o in self.desplazamientos]


def diseno(cfg: ConfigPipeline) -> DisenoSiembras:
    return DisenoSiembras([date(a, m, 1) for a, m in cfg.pronostico.inicios()], cfg.desplazamientos)


def _clim_ciclo(clim: F32, anio0: int, fechas: Sequence[date], dias: int) -> dict[str, F32]:
    y = np.clip(np.array([f.year - anio0 for f in fechas]), 0, clim.shape[0] - 1)
    doy = (np.array([indice_doy(f) for f in fechas])[:, None] + np.arange(dias)[None, :]) % 366
    c = clim[y[:, None], doy]
    return {"t": c[..., INDICE["t_media"]], "tmax": c[..., INDICE["tmax_media"]],
            "tmin": c[..., INDICE["tmin_media"]], "lluvia": c[..., INDICE["lluvia_media"]],
            "et0": c[..., INDICE["et0_media"]]}  # fmt: skip


def _real(clima: F32, idx: npt.NDArray[np.int64], dias: int) -> dict[str, F32]:
    pos = np.clip(idx[:, None] + np.arange(dias)[None, :], 0, clima.shape[0] - 1)
    return {k: clima[pos, _H[c]] for k, c in (("t", "t2m"), ("tmax", "tmax"), ("tmin", "tmin"),
                                               ("lluvia", "lluvia"), ("et0", "et0"))}  # fmt: skip


def _previo(clima: F32, idx: npt.NDArray[np.int64], dias: int) -> tuple[F32, F32]:
    pos = np.clip(idx[:, None] + np.arange(-dias, 0)[None, :], 0, clima.shape[0] - 1)
    return clima[pos, _H["lluvia"]], clima[pos, _H["et0"]]


def _ciclo(base: dict[str, F32], previo: tuple[F32, F32]) -> CicloClima:
    return CicloClima(base["t"], base["tmax"], base["tmin"], base["lluvia"], base["et0"], *previo)


def estadisticas_bf(resultados: F32, ciclo: int) -> F32:
    """[I, M, 6] del oráculo por miembro -> [I, len(PRIOR_BF)]."""
    r = resultados
    return np.stack([
        r[..., 0].mean(1), np.percentile(r[..., 0], 10, axis=1), np.percentile(r[..., 0], 90, axis=1),
        r[..., 1].mean(1), np.log1p(np.maximum(r[..., 2], 0)).mean(1), r[..., 3].mean(1) / ciclo,
        r[..., 4].mean(1) / ciclo, r[..., 5].mean(1),
    ], axis=-1).astype(np.float32)  # fmt: skip


@dataclass(frozen=True)
class TrabajoPunto:
    p: int
    id: str
    lat: float
    elevacion: float
    clima: F32
    valido: npt.NDArray[np.bool_]
    clim: F32
    anio0: int
    perfil: F32
    dia0: date
    fichas: list[FichaCultivo]
    cfg: ConfigPipeline
    carpeta_pron: Path
    salida: Path


def procesar_punto(tr: TrabajoPunto) -> str:
    if tr.salida.exists():
        return tr.id
    cfg, dis = tr.cfg, diseno(tr.cfg)
    crudo = np.load(ruta_crudo(tr.carpeta_pron), mmap_mode="r")
    hechos = np.load(tr.carpeta_pron / "hechos.npy")
    fc = np.asarray(crudo[:, tr.p], dtype=np.float32)  # [I, M, T, C]
    meses = np.array([d.month for d in dis.inicios])
    entreno = hechos & np.array([cfg.entrenamiento.contiene(d.year) for d in dis.inicios])
    obs = observado_alineado(tr.clima, tr.valido, tr.dia0, dis.inicios, fc.shape[2])
    cor = ajustar_correccion(np.where(hechos[:, None, None, None], fc, np.nan), obs, meses, entreno,
                             cfg.pronostico.bloque_lead_dias)  # fmt: skip
    diario = derivar_diario(aplicar_correccion(np.nan_to_num(fc), meses, cor), tr.lat, tr.elevacion, dis.inicios)
    anomalias = anomalias_pentadas(diario, tr.clim, tr.anio0, dis.inicios)

    fechas = dis.fechas()
    sow = np.array([(f - tr.dia0).days for f in fechas], dtype=np.int64)
    acumulado = np.concatenate([[0], np.cumsum(~tr.valido)])
    ph = float(media_entre(tr.perfil.astype(np.float64), 0, 30)[PROPIEDADES.index("phh2o")])
    previo = _previo(tr.clima, sow, cfg.dias_previos)
    S, C, K = dis.n, len(tr.fichas), len(dis.desplazamientos)
    etiquetas = np.zeros((S, C, len(ETIQUETAS)), np.float32)
    b0 = np.zeros_like(etiquetas)
    bf = np.zeros((S, C, len(PRIOR_BF)), np.float32)
    valido = np.zeros((S, C), bool)
    hecho_s = np.repeat(hechos, K)
    for c, ficha in enumerate(tr.fichas):
        L, oraculo = ficha.ciclo_dias, Oraculo(ficha, cfg.umbral_exito)
        taw = agua_disponible_mm(tr.perfil.astype(np.float64), ficha.zr)
        fin = sow + L
        dentro = (sow - DIAS_HISTORIA >= 0) & (fin <= tr.clima.shape[0])
        limpio = acumulado[np.clip(fin, 0, len(tr.valido))] - acumulado[np.clip(sow - DIAS_HISTORIA, 0, len(tr.valido))] == 0
        valido[:, c] = dentro & limpio & hecho_s
        etiquetas[:, c] = oraculo.simular(_ciclo(_real(tr.clima, sow, L), previo), taw, ph)
        clim_s = _clim_ciclo(tr.clim, tr.anio0, fechas, L)
        b0[:, c] = oraculo.simular(_ciclo(clim_s, previo), taw, ph)
        for k, o in enumerate(dis.desplazamientos):
            s_k = np.arange(len(dis.inicios)) * K + k
            n_fc = min(L, diario.shape[2] - o)
            M = diario.shape[1]
            base = {}
            for nombre, canal in (("t", "t2m"), ("tmax", "tmax"), ("tmin", "tmin"), ("lluvia", "lluvia"), ("et0", "et0")):
                parte_fc = diario[:, :, o : o + n_fc, _D[canal]]  # [I, M, n_fc]
                cola = np.broadcast_to(clim_s[nombre][s_k][:, None, n_fc:], (len(s_k), M, L - n_fc))
                base[nombre] = np.concatenate([parte_fc, cola], axis=2).reshape(len(s_k) * M, L)
            prev = tuple(np.repeat(x[s_k], M, axis=0) for x in previo)
            r = oraculo.simular(_ciclo(base, prev), taw, ph).reshape(len(s_k), M, -1)  # type: ignore[arg-type]
            bf[s_k, c] = estadisticas_bf(r, L)
    tr.salida.parent.mkdir(parents=True, exist_ok=True)
    tmp = tr.salida.with_suffix(".part.npz")
    np.savez(tmp, etiquetas=etiquetas, b0=b0, bf=bf, valido=valido, anomalias=anomalias.astype(np.float16),
             cor_normal=cor.normal, cor_cuantiles=cor.cuantiles)  # fmt: skip
    tmp.replace(tr.salida)
    return tr.id


def construir_etiquetas(datos: DatosLimpios, fichas: list[FichaCultivo], cfg: ConfigPipeline,
                        carpeta_pron: Path, carpeta: Path) -> None:
    trabajos = [TrabajoPunto(p, str(datos.ids[p]), float(datos.lat[p]), float(datos.elevacion[p]),
                             datos.clima[p], datos.valido[p], datos.clim[p], int(datos.anios_clim[0]),
                             datos.perfil_suelo[p], datos.dia0, fichas, cfg, carpeta_pron,
                             carpeta / "puntos" / f"{datos.ids[p]}.npz")
                for p in range(len(datos.ids))]  # fmt: skip
    # spawn: igual en Linux y Windows, y sin los bloqueos de fork() en procesos con hilos.
    with ProcessPoolExecutor(max_workers=max(cfg.procesos, 1), mp_context=multiprocessing.get_context("spawn")) as pool:
        for n, pid in enumerate(pool.map(procesar_punto, trabajos), start=1):
            log.info("etiquetas %d/%d %s", n, len(trabajos), pid)
    consolidar(datos, carpeta)


def consolidar(datos: DatosLimpios, carpeta: Path) -> None:
    """Junta los archivos por punto: etiquetas.npz, anomalias.npy (memmap) y correccion.npz."""
    rutas = [carpeta / "puntos" / f"{i}.npz" for i in datos.ids]
    with np.load(rutas[0]) as f:
        forma_a = f["anomalias"].shape
    anom = np.lib.format.open_memmap(carpeta / "anomalias.npy", mode="w+", dtype=np.float16,
                                     shape=(len(rutas), *forma_a))  # fmt: skip
    partes: dict[str, list[npt.NDArray[np.generic]]] = {k: [] for k in ("etiquetas", "b0", "bf", "valido",
                                                                        "cor_normal", "cor_cuantiles")}  # fmt: skip
    for p, ruta in enumerate(rutas):
        with np.load(ruta) as f:
            anom[p] = f["anomalias"]
            for k in partes:
                partes[k].append(f[k])
    anom.flush()
    np.savez(carpeta / "etiquetas.npz", **{k: np.stack(v) for k, v in partes.items() if not k.startswith("cor")})
    np.savez(carpeta / "correccion.npz", normal=np.stack(partes["cor_normal"]),
             cuantiles=np.stack(partes["cor_cuantiles"]), lat=datos.lat, lon=datos.lon,
             elevacion=datos.elevacion)  # fmt: skip
    log.info("etiquetas consolidadas: %d puntos", len(rutas))


@dataclass(frozen=True)
class Etiquetas:
    etiquetas: F32  # [P, S, C, 6]
    b0: F32  # [P, S, C, 6]
    bf: F32  # [P, S, C, 8]
    valido: npt.NDArray[np.bool_]  # [P, S, C]

    @classmethod
    def cargar(cls, carpeta: Path) -> "Etiquetas":
        with np.load(carpeta / "etiquetas.npz") as f:
            return cls(f["etiquetas"], f["b0"], f["bf"], f["valido"])


def correccion_de(carpeta: Path, p: int, bloque_dias: int) -> Correccion:
    with np.load(carpeta / "correccion.npz") as f:
        return Correccion(f["normal"][p], f["cuantiles"][p], bloque_dias)
