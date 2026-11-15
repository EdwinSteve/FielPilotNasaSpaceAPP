"""Elevación del punto con Open-Meteo (Copernicus DEM GLO-90), hasta 100 coordenadas por llamada."""

from collections.abc import Sequence

from surco_planificador.fuentes.http import HttpCache, url_con

URL = "https://api.open-meteo.com/v1/elevation"
LOTE = 100


def traer_elevaciones(http: HttpCache, coords: Sequence[tuple[float, float]]) -> list[float]:
    out: list[float] = []
    for i in range(0, len(coords), LOTE):
        trozo = coords[i : i + LOTE]
        url = url_con(URL, {
            "latitude": ",".join(f"{lat:.4f}" for lat, _ in trozo),
            "longitude": ",".join(f"{lon:.4f}" for _, lon in trozo),
        })  # fmt: skip
        out.extend(float(e) for e in http.json(url)["elevation"])
    return out
