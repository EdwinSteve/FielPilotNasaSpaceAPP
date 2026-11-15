"""NASA POWER, datos diarios por punto (comunidad AG). Es la verdad del terreno de las etiquetas."""

from dataclasses import dataclass
from datetime import date

import numpy as np
import numpy.typing as npt

from surco_planificador.fuentes.http import HttpCache, url_con

URL_POWER = "https://power.larc.nasa.gov/api/temporal/daily/point"
PARAMETROS = (
    "T2M", "T2M_MAX", "T2M_MIN", "T2MDEW", "RH2M",
    "PRECTOTCORR", "WS2M", "ALLSKY_SFC_SW_DWN", "GWETROOT",
)  # fmt: skip
VALOR_RELLENO = -999.0


@dataclass(frozen=True)
class SeriePower:
    dias: tuple[date, ...]
    valores: dict[str, npt.NDArray[np.float64]]  # NaN donde POWER pone su valor de relleno
    elevacion_celda_m: float


def url_power(lat: float, lon: float, primero: date, ultimo: date) -> str:
    return url_con(URL_POWER, {
        "parameters": ",".join(PARAMETROS), "community": "AG",
        "latitude": f"{lat:.4f}", "longitude": f"{lon:.4f}",
        "start": primero.strftime("%Y%m%d"), "end": ultimo.strftime("%Y%m%d"), "format": "JSON",
    })  # fmt: skip


def traer_power(http: HttpCache, lat: float, lon: float, primero: date, ultimo: date) -> SeriePower:
    datos = http.json(url_power(lat, lon, primero, ultimo))
    params = datos["properties"]["parameter"]
    claves = sorted(params[PARAMETROS[0]])
    dias = tuple(date(int(k[:4]), int(k[4:6]), int(k[6:])) for k in claves)
    valores = {}
    for nombre in PARAMETROS:
        # POWER omite un parámetro sin datos en el rango (la radiación antes de 1984): queda en NaN.
        columna = params.get(nombre, {})
        serie = np.array([columna.get(k, VALOR_RELLENO) for k in claves], dtype=np.float64)
        serie[serie <= VALOR_RELLENO] = np.nan
        valores[nombre] = serie
    return SeriePower(dias, valores, float(datos["geometry"]["coordinates"][2]))
