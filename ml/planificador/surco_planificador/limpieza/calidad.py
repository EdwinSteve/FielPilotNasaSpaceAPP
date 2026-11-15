"""Pasos de calidad de NASA POWER: rangos, consistencia, huecos cortos y corrección por elevación."""

import numpy as np
import numpy.typing as npt

A = npt.NDArray[np.float64]

RANGOS: dict[str, tuple[float, float]] = {
    "T2M": (-10.0, 50.0), "T2M_MAX": (-10.0, 50.0), "T2M_MIN": (-10.0, 50.0),
    "T2MDEW": (-30.0, 40.0), "RH2M": (0.0, 100.0), "PRECTOTCORR": (0.0, 400.0),
    "WS2M": (0.0, 40.0), "ALLSKY_SFC_SW_DWN": (0.0, 45.0), "GWETROOT": (0.0, 1.0),
}  # fmt: skip
NO_INTERPOLAR = frozenset({"PRECTOTCORR"})  # la lluvia nunca se inventa


def aplicar_rangos(valores: dict[str, A]) -> int:
    n = 0
    for nombre, (bajo, alto) in RANGOS.items():
        malo = (valores[nombre] < bajo) | (valores[nombre] > alto)
        n += int(malo.sum())
        valores[nombre][malo] = np.nan
    return n


def aplicar_consistencia(valores: dict[str, A]) -> int:
    t, tmax, tmin = valores["T2M"], valores["T2M_MAX"], valores["T2M_MIN"]
    malo = (tmin > t) | (t > tmax)
    for s in (t, tmax, tmin):
        s[malo] = np.nan
    return int(malo.sum())


def llenar_huecos_cortos(serie: A, max_hueco: int) -> int:
    """Interpolación lineal en huecos de hasta `max_hueco` días; los bordes quedan faltantes."""
    faltan = np.flatnonzero(np.isnan(serie))
    if len(faltan) == 0:
        return 0
    llenados = 0
    for tramo in np.split(faltan, np.flatnonzero(np.diff(faltan) > 1) + 1):
        a, b = int(tramo[0]), int(tramo[-1])
        if len(tramo) > max_hueco or a == 0 or b == len(serie) - 1:
            continue
        pasos = np.arange(1, len(tramo) + 1) / (len(tramo) + 1)
        serie[a : b + 1] = serie[a - 1] + (serie[b + 1] - serie[a - 1]) * pasos
        llenados += len(tramo)
    return llenados


def limpiar_valores(valores: dict[str, A], max_hueco: int) -> dict[str, int]:
    return {
        "fuera_de_rango": aplicar_rangos(valores),
        "dias_inconsistentes": aplicar_consistencia(valores),
        "interpolados": sum(llenar_huecos_cortos(s, max_hueco)
                            for n, s in valores.items() if n not in NO_INTERPOLAR),
    }  # fmt: skip


def ajuste_elevacion_c(punto_m: float, celda_m: float, gradiente_c_por_km: float) -> float:
    """Se suma a las temperaturas de POWER (positivo cuando el lote está más bajo que la celda)."""
    return gradiente_c_por_km * (punto_m - celda_m) / 1000
