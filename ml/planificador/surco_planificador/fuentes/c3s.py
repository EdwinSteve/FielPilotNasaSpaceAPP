"""Pronósticos estacionales de Copernicus C3S (ECMWF SEAS5, system 51), diarios a 1°.

Un pedido por inicio (año, mes): todos los miembros, leads de 24 h a 5160 h (215 días) y el
recuadro de Colombia. Necesita una cuenta del Climate Data Store, ~/.cdsapirc con la clave y
haber aceptado la licencia del dataset en su página. Un archivo ya descargado no se vuelve a pedir.

Las variables acumuladas (lluvia, radiación) vienen sumadas desde el inicio: se diferencian.
Las de temperatura máxima/mínima son de las últimas 24 h. Las instantáneas (punto de rocío,
viento) son las de 00 UTC; su sesgo frente al promedio diario lo absorbe la corrección contra POWER.
"""

import logging
import time
import zipfile
from pathlib import Path

import numpy as np
import numpy.typing as npt

from surco_planificador.config import ConfigPronostico

log = logging.getLogger(__name__)
F32 = npt.NDArray[np.float32]

VARIABLES = {  # nombre en el CDS -> nombre corto en el NetCDF
    "maximum_2m_temperature_in_the_last_24_hours": ("mx2t24", "tmax24", "max_2t"),
    "minimum_2m_temperature_in_the_last_24_hours": ("mn2t24", "tmin24", "min_2t"),
    "2m_dewpoint_temperature": ("d2m", "2d"),
    "total_precipitation": ("tp",),
    "surface_solar_radiation_downwards": ("ssrd",),
    "10m_u_component_of_wind": ("u10", "10u"),
    "10m_v_component_of_wind": ("v10", "10v"),
}
# Canales diarios crudos por miembro, en este orden.
CANALES_CRUDOS = ("tmax", "tmin", "tdew", "lluvia", "radiacion", "viento")
VIENTO_10_A_2 = 4.87 / np.log(67.8 * 10 - 5.42)  # FAO-56 Ec. 47


def pedido(cfg: ConfigPronostico, anio: int, mes: int) -> dict[str, object]:
    return {
        "originating_centre": cfg.centro,
        "system": cfg.sistema,
        "variable": list(VARIABLES),
        "year": [str(anio)],
        "month": [f"{mes:02d}"],
        "day": ["01"],
        "leadtime_hour": [str(h) for h in range(24, 24 * cfg.dias_lead + 1, 24)],
        "area": list(cfg.area),
        "data_format": "netcdf",
    }


def ruta_inicio(carpeta: Path, anio: int, mes: int) -> Path:
    return carpeta / f"seas5_{anio}{mes:02d}.nc"


def descargar_inicio(cfg: ConfigPronostico, carpeta: Path, anio: int, mes: int) -> Path:
    import cdsapi  # extra "datos"

    destino = ruta_inicio(carpeta, anio, mes)
    if destino.exists():
        return destino
    carpeta.mkdir(parents=True, exist_ok=True)
    tmp = destino.with_suffix(".part")
    while True:
        try:
            cdsapi.Client(quiet=True).retrieve(cfg.dataset, pedido(cfg, anio, mes), str(tmp))
            break
        except Exception as e:  # el CDS rechaza el pedido si la cola por usuario del dataset está llena
            texto = f"{e} {getattr(getattr(e, 'response', None), 'text', '')}".lower()
            if "maximum number queued" not in texto:
                raise
            log.info("C3S %d-%02d: cola del CDS llena, reintento en 2 min", anio, mes)
            time.sleep(120)
    tmp.replace(destino)
    log.info("C3S %d-%02d descargado (%.0f MB)", anio, mes, destino.stat().st_size / 1e6)
    return destino


def _abrir(ruta: Path) -> list[object]:
    """El CDS puede devolver un .zip con un NetCDF por tipo de paso (instantáneo, acumulado...)."""
    import xarray as xr

    if not zipfile.is_zipfile(ruta):
        return [xr.open_dataset(ruta, decode_timedelta=True)]
    carpeta = ruta.with_suffix(".d")
    if not carpeta.exists():
        with zipfile.ZipFile(ruta) as z:
            z.extractall(carpeta)
    return [xr.open_dataset(p, decode_timedelta=True) for p in sorted(carpeta.glob("*.nc"))]


def _normalizar(da: object, miembros: int) -> tuple[F32, F32, F32]:
    """DataArray -> [miembro, lead, lat, lon] float32, con lat y lon."""
    dims = list(da.dims)  # type: ignore[attr-defined]
    miembro = next(d for d in dims if d in ("number", "realization", "member"))
    lead = next(d for d in dims if d in ("forecast_period", "step", "leadtime", "time", "valid_time"))
    lat = next(d for d in dims if d.startswith("lat"))
    lon = next(d for d in dims if d.startswith("lon"))
    sobran = [d for d in dims if d not in (miembro, lead, lat, lon)]
    da = da.isel({d: 0 for d in sobran}).sortby(lead).transpose(miembro, lead, lat, lon)  # type: ignore[attr-defined]
    valores = np.asarray(da.values, dtype=np.float32)[:miembros]
    if valores.shape[0] < miembros:  # rellena con NaN; el miembro queda marcado como faltante
        relleno = np.full((miembros - valores.shape[0], *valores.shape[1:]), np.nan, np.float32)
        valores = np.concatenate([valores, relleno])
    return valores, np.asarray(da[lat].values, np.float32), np.asarray(da[lon].values, np.float32)  # type: ignore[index]


def leer_inicio(ruta: Path, miembros: int, dias: int) -> tuple[F32, F32, F32]:
    """[miembro, día, lat, lon, CANALES_CRUDOS] en unidades de POWER (°C, mm/día, MJ/m²/día, m/s a 2 m)."""
    crudos: dict[str, F32] = {}
    lats = lons = np.zeros(0, np.float32)
    for ds in _abrir(ruta):
        try:
            for alias in VARIABLES.values():
                nombre = next((a for a in alias if a in ds.data_vars), None)  # type: ignore[attr-defined]
                if nombre is not None:
                    crudos[alias[0]], lats, lons = _normalizar(ds[nombre], miembros)  # type: ignore[index]
        finally:
            ds.close()  # type: ignore[attr-defined]  # Windows limita los archivos abiertos
    faltan = [a[0] for a in VARIABLES.values() if a[0] not in crudos]
    if faltan:
        raise ValueError(f"{ruta.name}: faltan variables {faltan}")

    def diario_de_acumulado(x: F32) -> F32:
        return np.maximum(np.diff(x, axis=1, prepend=0.0), 0.0)

    canales = {
        "tmax": crudos["mx2t24"] - 273.15,
        "tmin": crudos["mn2t24"] - 273.15,
        "tdew": crudos["d2m"] - 273.15,
        "lluvia": diario_de_acumulado(crudos["tp"]) * 1000.0,
        "radiacion": diario_de_acumulado(crudos["ssrd"]) / 1e6,
        "viento": np.hypot(crudos["u10"], crudos["v10"]) * VIENTO_10_A_2,
    }
    out = np.stack([canales[c][:, :dias] for c in CANALES_CRUDOS], axis=-1).astype(np.float32)
    return out, lats, lons


def pesos_bilineales(lats: F32, lons: F32, lat: float, lon: float) -> tuple[list[tuple[int, int]], list[float]]:
    """Las cuatro celdas que rodean el punto y sus pesos (la grilla puede venir de norte a sur)."""

    def eje(valores: F32, x: float) -> tuple[int, int, float]:
        orden = np.argsort(valores)
        v = valores[orden]
        j = int(np.clip(np.searchsorted(v, x) - 1, 0, len(v) - 2))
        t = float(np.clip((x - v[j]) / (v[j + 1] - v[j]), 0.0, 1.0))
        return int(orden[j]), int(orden[j + 1]), t

    i0, i1, ty = eje(lats, lat)
    j0, j1, tx = eje(lons, lon)
    celdas = [(i0, j0), (i0, j1), (i1, j0), (i1, j1)]
    pesos = [(1 - ty) * (1 - tx), (1 - ty) * tx, ty * (1 - tx), ty * tx]
    return celdas, pesos


def interpolar(campo: F32, lats: F32, lons: F32, puntos: list[tuple[float, float]]) -> F32:
    """campo [M, T, lat, lon, C] -> [P, M, T, C] bilineal en cada punto."""
    out = np.empty((len(puntos), campo.shape[0], campo.shape[1], campo.shape[-1]), np.float32)
    for k, (lat, lon) in enumerate(puntos):
        celdas, pesos = pesos_bilineales(lats, lons, lat, lon)
        out[k] = sum(w * campo[:, :, i, j] for (i, j), w in zip(celdas, pesos, strict=True))
    return out
