"""Climatología diaria sin fuga: deja fuera el año de siembra Y el siguiente.

Un ciclo sembrado en noviembre termina el año siguiente; dejar fuera solo el año de siembra (como
hacía FieldPilot) metía parte del clima real del ciclo en su propia climatología. Aquí se excluyen
los dos años que el ciclo y el pronóstico (215 días) pueden tocar.
"""

from datetime import date

import numpy as np
import numpy.typing as npt

from surco_planificador.config import Anios

A = npt.NDArray[np.float64]
DOY = 366
SALIDAS = ("t_media", "t_p10", "t_p90", "lluvia_media", "lluvia_p10", "lluvia_p90",
           "tmax_media", "tmin_media", "et0_media")  # fmt: skip
INDICE = {n: i for i, n in enumerate(SALIDAS)}


def media_movil(serie: A, ventana: int) -> A:
    """Media móvil centrada que ignora NaN."""
    validos = np.isfinite(serie)
    nucleo = np.ones(ventana)
    sumas = np.convolve(np.where(validos, serie, 0.0), nucleo, mode="same")
    cuentas = np.convolve(validos.astype(float), nucleo, mode="same")
    return np.where(cuentas > 0, sumas / np.maximum(cuentas, 1), np.nan)


def por_dia_del_anio(suavizada: A, dia0: date, anios: range) -> A:
    """[año, 366]: el día 366 repite el 365 en años no bisiestos."""
    out = np.full((len(anios), DOY), np.nan)
    for i, a in enumerate(anios):
        ini = (date(a, 1, 1) - dia0).days
        largo = (date(a + 1, 1, 1) - date(a, 1, 1)).days
        bloque = suavizada[ini : ini + largo]
        out[i, :largo] = bloque
        if largo == DOY - 1:
            out[i, DOY - 1] = bloque[-1]
    return out


def sin_fuga(series: dict[str, A], dia0: date, base: Anios, anios_etiqueta: range, ventana: int
             ) -> npt.NDArray[np.float32]:
    """[año de etiqueta, 366, len(SALIDAS)] de un punto. series: t, lluvia, tmax, tmin, et0."""
    anios_base = np.arange(base.primero, base.ultimo + 1)
    tablas = {k: por_dia_del_anio(media_movil(v, ventana), dia0, range(base.primero, base.ultimo + 1))
              for k, v in series.items()}  # fmt: skip
    out = np.empty((len(anios_etiqueta), DOY, len(SALIDAS)), np.float32)
    for i, a in enumerate(anios_etiqueta):
        out[i] = estadisticas(tablas, (anios_base != a) & (anios_base != a + 1))
    return out


def servida(series: dict[str, A], dia0: date, base: Anios, ventana: int) -> npt.NDArray[np.float32]:
    """[366, len(SALIDAS)] con todos los años base: lo que recibe un lote en producción."""
    tablas = {k: por_dia_del_anio(media_movil(v, ventana), dia0, range(base.primero, base.ultimo + 1))
              for k, v in series.items()}  # fmt: skip
    return estadisticas(tablas, np.ones(base.ultimo - base.primero + 1, dtype=bool))


def estadisticas(tablas: dict[str, A], usar: npt.NDArray[np.bool_]) -> npt.NDArray[np.float32]:
    t, ll = tablas["t"][usar], tablas["lluvia"][usar]
    return np.stack([
        np.nanmean(t, 0), np.nanpercentile(t, 10, 0), np.nanpercentile(t, 90, 0),
        np.nanmean(ll, 0), np.nanpercentile(ll, 10, 0), np.nanpercentile(ll, 90, 0),
        np.nanmean(tablas["tmax"][usar], 0), np.nanmean(tablas["tmin"][usar], 0),
        np.nanmean(tablas["et0"][usar], 0),
    ], axis=-1).astype(np.float32)  # fmt: skip


def indice_doy(dia: date) -> int:
    """Índice 0..365; el 31 de diciembre de un año no bisiesto es 364."""
    return (dia - date(dia.year, 1, 1)).days
