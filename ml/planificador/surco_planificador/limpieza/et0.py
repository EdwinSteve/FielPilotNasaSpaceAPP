"""Evapotranspiración de referencia FAO-56 (Allen et al., 1998), vectorizada. Ecuaciones numeradas como FAO-56."""

import numpy as np
import numpy.typing as npt

A = npt.NDArray[np.floating]
SIGMA = 4.903e-9  # Stefan-Boltzmann, MJ K-4 m-2 día-1


def presion_saturacion(t_c: A) -> A:
    """Ec. 11, kPa."""
    return np.asarray(0.6108 * np.exp(17.27 * t_c / (t_c + 237.3)))


def radiacion_extraterrestre(lat: float, doy: A) -> A:
    """Ra, Ecs. 21-25, MJ m-2 día-1."""
    phi = np.radians(lat)
    dr = 1 + 0.033 * np.cos(2 * np.pi * doy / 365)
    delta = 0.409 * np.sin(2 * np.pi * doy / 365 - 1.39)
    ws = np.arccos(np.clip(-np.tan(phi) * np.tan(delta), -1.0, 1.0))
    return np.asarray(24 * 60 / np.pi * 0.0820 * dr
                      * (ws * np.sin(phi) * np.sin(delta) + np.cos(phi) * np.cos(delta) * np.sin(ws)))  # fmt: skip


def penman_monteith(tmax: A, tmin: A, ea: A, u2: A, rs: A, lat: float, elev_m: float, doy: A) -> A:
    """Ec. 6 con G = 0 en pasos diarios, mm/día."""
    tmedia = (tmax + tmin) / 2
    presion = 101.3 * ((293 - 0.0065 * elev_m) / 293) ** 5.26  # Ec. 7
    gamma = 0.000665 * presion  # Ec. 8
    es = (presion_saturacion(tmax) + presion_saturacion(tmin)) / 2  # Ec. 12
    pendiente = 4098 * presion_saturacion(tmedia) / (tmedia + 237.3) ** 2  # Ec. 13
    ra = radiacion_extraterrestre(lat, doy)
    rso = (0.75 + 2e-5 * elev_m) * ra  # Ec. 37
    rns = 0.77 * rs  # Ec. 38, albedo 0,23
    relativa = np.clip(rs / np.where(rso > 0, rso, np.nan), 0.0, 1.0)
    rnl = (SIGMA * ((tmax + 273.16) ** 4 + (tmin + 273.16) ** 4) / 2
           * (0.34 - 0.14 * np.sqrt(np.maximum(ea, 0.0))) * (1.35 * relativa - 0.35))  # Ec. 39  # fmt: skip
    rn = rns - rnl
    num = 0.408 * pendiente * rn + gamma * 900 / (tmedia + 273) * u2 * (es - ea)
    return np.asarray(np.maximum(num / (pendiente + gamma * (1 + 0.34 * u2)), 0.0))


def hargreaves(tmax: A, tmin: A, lat: float, doy: A) -> A:
    """Ec. 52, mm/día: respaldo cuando falta la radiación."""
    ra = radiacion_extraterrestre(lat, doy)
    return np.asarray(np.maximum(
        0.0023 * ((tmax + tmin) / 2 + 17.8) * np.sqrt(np.maximum(tmax - tmin, 0.0)) * 0.408 * ra, 0.0))  # fmt: skip


def radiacion_desde_temperatura(tmax: A, tmin: A, lat: float, doy: A) -> A:
    """Ec. 50 con kRs = 0,16 (interior), MJ m-2 día-1."""
    return np.asarray(0.16 * np.sqrt(np.maximum(tmax - tmin, 0.0)) * radiacion_extraterrestre(lat, doy))
