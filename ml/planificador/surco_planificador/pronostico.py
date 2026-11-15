"""Pronóstico estacional en cada punto: interpolación, corrección de sesgo y anomalías por péntada.

Tres reglas para que la red no haga trampa ni aprenda el sesgo del modelo climático:
1. Solo se usa el pronóstico que existía al sembrar: el iniciado el día 1 se publica el 6 (C3S) y
   las siembras de entrenamiento caen entre los días 7 y 32 desde el inicio (pipeline.yaml).
2. La corrección (media y desviación por mes de inicio y bloque de lead; mapeo de cuantiles en la
   lluvia) se ajusta contra POWER en el punto y SOLO con años de entrenamiento.
3. Las anomalías se miden contra una climatología que excluye el año del inicio y el siguiente.
"""

import logging
from dataclasses import dataclass
from datetime import date, timedelta
from pathlib import Path

import numpy as np
import numpy.typing as npt

from surco_planificador.caracteristicas import (
    CANALES_ANOMALIA,
    CANALES_HISTORIA,
    CANALES_PRONOSTICO_DIARIO,
    CLIM_DE_ANOMALIA,
    PENTADA,
    PENTADAS_GUARDADAS,
)
from surco_planificador.config import ConfigPronostico
from surco_planificador.datos import DatosLimpios
from surco_planificador.fuentes.c3s import CANALES_CRUDOS, interpolar, leer_inicio, ruta_inicio
from surco_planificador.limpieza.climatologia import INDICE, indice_doy
from surco_planificador.limpieza.et0 import penman_monteith, presion_saturacion

log = logging.getLogger(__name__)
F32 = npt.NDArray[np.float32]
CUANTILES = np.linspace(0, 1, 101)
_C = {c: i for i, c in enumerate(CANALES_CRUDOS)}
_H = {c: i for i, c in enumerate(CANALES_HISTORIA)}
LLUVIA = _C["lluvia"]


# --- Etapa `interpolar`: los NetCDF de C3S -> un memmap [inicio, punto, miembro, día, canal] -------

def ruta_crudo(carpeta: Path) -> Path:
    return carpeta / "crudo.npy"


def interpolar_todo(cfg: ConfigPronostico, carpeta_nc: Path, carpeta: Path,
                    puntos: list[tuple[float, float]]) -> None:
    inicios = cfg.inicios()
    forma = (len(inicios), len(puntos), cfg.miembros, cfg.dias_lead, len(CANALES_CRUDOS))
    ruta, ruta_hechos = ruta_crudo(carpeta), carpeta / "hechos.npy"
    carpeta.mkdir(parents=True, exist_ok=True)
    if ruta.exists():
        crudo = np.load(ruta, mmap_mode="r+")
        if crudo.shape != forma:
            raise ValueError(f"{ruta} tiene forma {crudo.shape}; se esperaba {forma}: bórrelo y repita")
        hechos = np.load(ruta_hechos)
    else:
        crudo = np.lib.format.open_memmap(ruta, mode="w+", dtype=np.float16, shape=forma)
        hechos = np.zeros(len(inicios), dtype=bool)
    for i, (anio, mes) in enumerate(inicios):
        nc = ruta_inicio(carpeta_nc, anio, mes)
        if hechos[i] or not nc.exists():
            continue
        campo, lats, lons = leer_inicio(nc, cfg.miembros, cfg.dias_lead)
        crudo[i] = interpolar(campo, lats, lons, puntos).astype(np.float16)
        hechos[i] = bool(np.isfinite(crudo[i]).all())
        np.save(ruta_hechos, hechos)
        log.info("interpolado %d-%02d (%d/%d)", anio, mes, int(hechos.sum()), len(inicios))
    crudo.flush()
    faltan = [f"{a}-{m:02d}" for (a, m), h in zip(inicios, hechos, strict=True) if not h]
    if faltan:
        log.warning("%d inicios sin pronóstico (sus siembras quedan fuera): %s", len(faltan), faltan[:12])


def analogos_todo(cfg: ConfigPronostico, datos: DatosLimpios, carpeta: Path) -> None:
    """Conjunto por años análogos (ESP condicionado por ONI), sin descargas: para cada inicio, los
    `miembros` años de POWER cuyo ONI conocido al inicio (y su tendencia) más se parece, con su
    trayectoria observada desde el mismo día del año. Se excluyen el año del inicio y sus vecinos,
    así ningún miembro contiene el clima que la etiqueta usa como verdad."""
    from surco_planificador.caracteristicas import oni_conocido

    inicios = cfg.inicios()
    P, M, T = len(datos.lat), cfg.miembros, cfg.dias_lead
    canales = [CANALES_HISTORIA.index(c) for c in CANALES_CRUDOS]
    carpeta.mkdir(parents=True, exist_ok=True)
    crudo = np.lib.format.open_memmap(ruta_crudo(carpeta), mode="w+", dtype=np.float16,
                                      shape=(len(inicios), P, M, T, len(CANALES_CRUDOS)))  # fmt: skip
    hechos = np.zeros(len(inicios), dtype=bool)
    oni = datos.mapa_oni()
    primero, ultimo = datos.dia0.year, datos.dia(datos.dias - 1).year

    def clave(fecha: date) -> tuple[float, float] | None:
        try:
            return oni_conocido(oni, fecha)
        except KeyError:
            return None

    for i, (anio, mes) in enumerate(inicios):
        ref = clave(date(anio, mes, 1))
        if ref is None:
            continue
        candidatos: list[tuple[float, int]] = []
        for y in range(primero, ultimo + 1):
            if abs(y - anio) <= 1:
                continue
            s = datos.indice(date(y, mes, 1))
            k = clave(date(y, mes, 1))
            if s < 0 or s + T > datos.dias or k is None:
                continue
            candidatos.append((abs(k[0] - ref[0]) + 0.5 * abs(k[1] - ref[1]), s))
        if len(candidatos) < M:
            continue
        elegidos = [s for _, s in sorted(candidatos)[:M]]
        crudo[i] = np.stack([datos.clima[:, s:s + T][:, :, canales] for s in elegidos], axis=1).astype(np.float16)
        hechos[i] = bool(np.isfinite(crudo[i]).all())
    crudo.flush()
    np.save(carpeta / "hechos.npy", hechos)
    log.info("análogos: %d de %d inicios", int(hechos.sum()), len(inicios))


# --- Corrección de sesgo ----------------------------------------------------------------------

@dataclass(frozen=True)
class Correccion:
    """Parámetros de un punto. normal: [12 meses, B bloques, C, 4] (media_f, desv_f, media_o, desv_o);
    cuantiles: [12, B, 2, Q] (pronóstico, observado) de la lluvia."""

    normal: F32
    cuantiles: F32
    bloque_dias: int

    def bloque(self, dias: int) -> npt.NDArray[np.int64]:
        return np.minimum(np.arange(dias) // self.bloque_dias, self.normal.shape[1] - 1)


def observado_alineado(clima: F32, valido: npt.NDArray[np.bool_], dia0: date,
                       fechas_inicio: list[date], dias: int) -> F32:
    """POWER en las fechas válidas de cada inicio: [I, días, CANALES_CRUDOS]; NaN fuera de rango."""
    indices = np.array([(d - dia0).days for d in fechas_inicio])[:, None] + np.arange(dias)[None, :]
    dentro = (indices >= 0) & (indices < clima.shape[0])
    seguro = np.clip(indices, 0, clima.shape[0] - 1)
    columnas = [_H[c] for c in CANALES_CRUDOS]
    obs = clima[seguro][..., columnas].astype(np.float32)
    obs[~(dentro & valido[seguro])] = np.nan
    return obs


def ajustar_correccion(fc: F32, obs: F32, meses: npt.NDArray[np.int64], usar: npt.NDArray[np.bool_],
                       bloque_dias: int) -> Correccion:
    """fc [I, M, T, C], obs [I, T, C]; `usar` marca los inicios de entrenamiento."""
    n_bloques = int(np.ceil(fc.shape[2] / bloque_dias))
    normal = np.zeros((12, n_bloques, fc.shape[-1], 4), np.float32)
    cuant = np.zeros((12, n_bloques, 2, len(CUANTILES)), np.float32)
    for mes in range(12):
        if not (meses == mes + 1).any():  # mes de inicio fuera de la configuración: sin corrección
            continue
        sel = usar & (meses == mes + 1)
        if not sel.any():
            raise ValueError(f"no hay inicios de entrenamiento para el mes {mes + 1}")
        for b in range(n_bloques):
            dias = slice(b * bloque_dias, min((b + 1) * bloque_dias, fc.shape[2]))
            f = fc[sel][:, :, dias]  # [i, M, t, C]
            o = obs[sel][:, dias]  # [i, t, C]
            for c in range(fc.shape[-1]):
                fv, ov = f[..., c].ravel(), o[..., c].ravel()
                fv, ov = fv[np.isfinite(fv)], ov[np.isfinite(ov)]
                normal[mes, b, c] = (fv.mean(), max(fv.std(), 1e-3), ov.mean(), max(ov.std(), 1e-3))
                if c == LLUVIA:
                    cuant[mes, b, 0] = np.quantile(fv, CUANTILES)
                    cuant[mes, b, 1] = np.quantile(ov, CUANTILES)
    return Correccion(normal, cuant, bloque_dias)


def mapear_cuantiles(x: F32, qf: F32, qo: F32) -> F32:
    """Mapeo empírico de cuantiles; por encima del máximo, extrapola multiplicando."""
    estrictos = qf + np.arange(len(qf), dtype=np.float32) * 1e-6  # np.interp pide x crecientes
    y = np.interp(x, estrictos, qo).astype(np.float32)
    arriba = x > qf[-1]
    escala = qo[-1] / max(float(qf[-1]), 1e-3)
    y[arriba] = x[arriba] * escala
    return np.maximum(y, 0.0)


def aplicar_correccion(fc: F32, meses: npt.NDArray[np.int64], cor: Correccion) -> F32:
    out = np.empty_like(fc, dtype=np.float32)
    bloques = cor.bloque(fc.shape[2])
    for i in range(fc.shape[0]):
        n = cor.normal[meses[i] - 1][bloques]  # [T, C, 4]
        out[i] = (fc[i] - n[..., 0]) / n[..., 1] * n[..., 3] + n[..., 2]
        for b in np.unique(bloques):
            t = bloques == b
            qf, qo = cor.cuantiles[meses[i] - 1, b]
            out[i][:, t, LLUVIA] = mapear_cuantiles(fc[i][:, t, LLUVIA].astype(np.float32), qf, qo)
    out[..., _C["tmin"]] = np.minimum(out[..., _C["tmin"]], out[..., _C["tmax"]] - 0.1)
    media = (out[..., _C["tmax"]] + out[..., _C["tmin"]]) / 2
    out[..., _C["tdew"]] = np.minimum(out[..., _C["tdew"]], media)
    out[..., _C["radiacion"]] = np.maximum(out[..., _C["radiacion"]], 0.0)
    out[..., _C["viento"]] = np.maximum(out[..., _C["viento"]], 0.0)
    return out


def derivar_diario(cor: F32, lat: float, elev_m: float, fechas_inicio: list[date]) -> F32:
    """Corregido [I, M, T, CRUDOS] -> [I, M, T, CANALES_PRONOSTICO_DIARIO] con ET0 Penman-Monteith."""
    tmax, tmin = cor[..., _C["tmax"]], cor[..., _C["tmin"]]
    t2m = (tmax + tmin) / 2
    ea = presion_saturacion(cor[..., _C["tdew"]])
    hr = np.clip(100 * ea / presion_saturacion(t2m), 0, 100)
    doy = np.array([[(d + timedelta(days=t)).timetuple().tm_yday for t in range(cor.shape[2])]
                    for d in fechas_inicio], dtype=np.float32)[:, None, :]  # fmt: skip
    et0 = penman_monteith(tmax, tmin, ea, cor[..., _C["viento"]], cor[..., _C["radiacion"]], lat, elev_m, doy)
    canales = {"t2m": t2m, "tmax": tmax, "tmin": tmin, "lluvia": cor[..., LLUVIA], "et0": et0, "hr": hr}
    return np.stack([canales[c] for c in CANALES_PRONOSTICO_DIARIO], -1).astype(np.float32)


def anomalias_pentadas(diario: F32, clim: F32, anio0: int, fechas_inicio: list[date]) -> F32:
    """[I, M, PENTADAS_GUARDADAS, CANALES_ANOMALIA]: pronóstico menos climatología sin fuga.

    La péntada j cubre los días 1+5j .. 5+5j desde el inicio; una siembra en el día 6+5k empieza
    justo en la péntada k+1, así las siembras de entrenamiento quedan alineadas sin interpolar.
    """
    dias = 1 + np.arange(PENTADAS_GUARDADAS * PENTADA)
    d = {c: i for i, c in enumerate(CANALES_PRONOSTICO_DIARIO)}
    out = np.empty((diario.shape[0], diario.shape[1], PENTADAS_GUARDADAS, len(CANALES_ANOMALIA)), np.float32)
    for i, ini in enumerate(fechas_inicio):
        y = int(np.clip(ini.year - anio0, 0, clim.shape[0] - 1))
        doy = np.array([indice_doy(ini + timedelta(days=int(t))) for t in dias])
        for k, canal in enumerate(CANALES_ANOMALIA):
            referencia = clim[y, doy, INDICE[CLIM_DE_ANOMALIA[canal]]]
            a = diario[i][:, dias, d[canal]] - referencia[None, :]  # [M, días]
            out[i, :, :, k] = a.reshape(a.shape[0], PENTADAS_GUARDADAS, PENTADA).mean(-1)
    return out
