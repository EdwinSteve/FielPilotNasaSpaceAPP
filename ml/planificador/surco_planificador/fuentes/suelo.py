"""ISRIC SoilGrids 2.0 desde los ráster agregados a 1 km (la API REST tarda ~2 min por punto).

Cada capa se lee una vez, recortada a Colombia, y se guarda en caché. Un punto toma el píxel más
cercano con valor en todas las capas (los centroides municipales suelen caer en cascos urbanos).
"""

import json
import math
from collections.abc import Sequence
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import numpy.typing as npt

URL_COG = "https://files.isric.org/soilgrids/latest/data_aggregated/1000m/{p}/{p}_{d}_mean_1000.tif"
PROPIEDADES = ("sand", "clay", "soc", "phh2o", "bdod", "wv0033", "wv1500", "nitrogen", "cec")
PROFUNDIDADES = ("0-5cm", "5-15cm", "15-30cm", "30-60cm", "60-100cm", "100-200cm")
LIMITES_CM = ((0, 5), (5, 15), (15, 30), (30, 60), (60, 100), (100, 200))
# Unidades del mapa ÷ factor = unidades convencionales: % ; g/kg ; pH ; kg/dm³ ; % vol ; g/kg ; cmol(c)/kg
FACTOR = {"sand": 10, "clay": 10, "soc": 10, "phh2o": 10, "bdod": 100, "wv0033": 10,
          "wv1500": 10, "nitrogen": 100, "cec": 10}  # fmt: skip
SIN_DATO = -32768


@dataclass(frozen=True)
class PerfilSuelo:
    lat: float  # del píxel usado
    lon: float
    capas: dict[str, tuple[float, ...]]  # propiedad -> un valor por profundidad


def mas_cercano_valido(validos: npt.NDArray[np.bool_], fila: int, col: int, radio: int
                       ) -> tuple[int, int] | None:
    f0, c0 = max(fila - radio, 0), max(col - radio, 0)
    ventana = validos[f0 : fila + radio + 1, c0 : col + radio + 1]
    fs, cs = np.nonzero(ventana)
    if len(fs) == 0:
        return None
    d = (fs + f0 - fila) ** 2 + (cs + c0 - col) ** 2
    i = int(np.argmin(d))
    return None if d[i] > radio**2 else (int(fs[i] + f0), int(cs[i] + c0))


class RasterSoilGrids:
    """Requiere la extra `datos` (rasterio con GDAL)."""

    def __init__(self, cache: Path, limites: tuple[float, float, float, float]) -> None:
        self._cache = cache
        self._limites = limites  # lat_min, lat_max, lon_min, lon_max
        cache.mkdir(parents=True, exist_ok=True)

    def _capa(self, prop: str, prof: str) -> tuple[npt.NDArray[np.int16], tuple[float, ...], str]:
        area = "_".join(f"{b:.2f}" for b in self._limites)
        ruta = self._cache / f"{prop}_{prof}_{area}.npz"
        if not ruta.exists():
            datos, transf, crs = self._leer(URL_COG.format(p=prop, d=prof))
            tmp = ruta.with_suffix(".part.npz")
            np.savez_compressed(tmp, data=datos, transform=np.array(transf), crs=crs)
            tmp.replace(ruta)
        with np.load(ruta) as f:
            return f["data"], tuple(float(v) for v in f["transform"]), str(f["crs"])

    def perfiles(self, coords: Sequence[tuple[float, float]], radio_km: float) -> list[PerfilSuelo | None]:
        import rasterio.transform
        from rasterio.warp import transform as reproyectar

        capas = {(p, d): self._capa(p, d) for p in PROPIEDADES for d in PROFUNDIDADES}
        _, transf, crs = capas[(PROPIEDADES[0], PROFUNDIDADES[0])]
        validos = np.logical_and.reduce([c[0] != SIN_DATO for c in capas.values()])
        afin = rasterio.transform.Affine(*transf[:6])
        xs, ys = reproyectar("EPSG:4326", crs, [lon for _, lon in coords], [lat for lat, _ in coords])
        radio = math.ceil(radio_km)
        out: list[PerfilSuelo | None] = []
        for x, y in zip(xs, ys, strict=True):
            fila, col = rasterio.transform.rowcol(afin, x, y)
            hallado = mas_cercano_valido(validos, int(fila), int(col), radio)
            if hallado is None:
                out.append(None)
                continue
            px, py = rasterio.transform.xy(afin, hallado[0], hallado[1])
            lon, lat = reproyectar(crs, "EPSG:4326", [px], [py])
            valores = {p: tuple(float(capas[(p, d)][0][hallado]) / FACTOR[p] for d in PROFUNDIDADES)
                       for p in PROPIEDADES}  # fmt: skip
            out.append(PerfilSuelo(float(lat[0]), float(lon[0]), valores))
        return out

    def _leer(self, url: str) -> tuple[npt.NDArray[np.int16], tuple[float, ...], str]:
        import rasterio
        from rasterio.warp import transform_bounds
        from rasterio.windows import from_bounds

        lat_min, lat_max, lon_min, lon_max = self._limites
        entorno = {"GDAL_DISABLE_READDIR_ON_OPEN": "EMPTY_DIR", "GDAL_HTTP_MAX_RETRY": "5",
                   "GDAL_HTTP_RETRY_DELAY": "5"}  # fmt: skip
        with rasterio.Env(**entorno), rasterio.open(f"/vsicurl/{url}") as ds:
            izq, abajo, der, arriba = transform_bounds("EPSG:4326", ds.crs, lon_min, lat_min, lon_max, lat_max)
            ventana = from_bounds(izq, abajo, der, arriba, ds.transform).round_offsets().round_lengths()
            datos = ds.read(1, window=ventana, boundless=True, fill_value=SIN_DATO).astype(np.int16)
            t = ds.window_transform(ventana)
            return datos, (t.a, t.b, t.c, t.d, t.e, t.f), ds.crs.to_wkt()


def guardar_perfil(ruta: Path, perfil: PerfilSuelo | None) -> None:
    datos = None if perfil is None else {"lat": perfil.lat, "lon": perfil.lon, "capas": perfil.capas}
    ruta.write_text(json.dumps(datos), encoding="utf-8")


def cargar_perfil(ruta: Path) -> PerfilSuelo | None:
    r = json.loads(ruta.read_text(encoding="utf-8"))
    return None if r is None else PerfilSuelo(r["lat"], r["lon"], {k: tuple(v) for k, v in r["capas"].items()})


def pesos_profundidad(arriba_cm: float, abajo_cm: float) -> npt.NDArray[np.float64]:
    solape = np.array([max(0.0, min(abajo_cm, b) - max(arriba_cm, a)) for a, b in LIMITES_CM])
    if solape.sum() <= 0:
        raise ValueError(f"no hay capa de suelo entre {arriba_cm} y {abajo_cm} cm")
    return solape / solape.sum()


def media_entre(perfil: npt.NDArray[np.float64], arriba_cm: float, abajo_cm: float) -> npt.NDArray[np.float64]:
    """perfil [..., capas, propiedades] -> [..., propiedades]."""
    return np.asarray(np.einsum("...lp,l->...p", perfil, pesos_profundidad(arriba_cm, abajo_cm)))


def agua_disponible_mm(perfil: npt.NDArray[np.float64], zr_m: float) -> float:
    """FAO-56 Ec. 82: TAW = 1000 (θCC − θPMP) Zr, con θ de wv0033 y wv1500 (% vol)."""
    medias = media_entre(perfil, 0, min(zr_m * 100, LIMITES_CM[-1][1]))
    cc, pmp = medias[PROPIEDADES.index("wv0033")], medias[PROPIEDADES.index("wv1500")]
    return float(max(cc - pmp, 1.0) / 100 * 1000 * zr_m)
