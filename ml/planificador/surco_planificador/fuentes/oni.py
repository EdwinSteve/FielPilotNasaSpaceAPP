"""Índice Oceánico El Niño (ONI) de NOAA CPC."""

from surco_planificador.fuentes.http import HttpCache

URL_ONI = "https://www.cpc.ncep.noaa.gov/data/indices/oni.ascii.txt"
TRIMESTRES = ("DJF", "JFM", "FMA", "MAM", "AMJ", "MJJ", "JJA", "JAS", "ASO", "SON", "OND", "NDJ")


def leer_oni(texto: str) -> dict[tuple[int, int], float]:
    """Anomalía por (año, mes en que TERMINA el trimestre). "NDJ 1950" termina en enero de 1951."""
    out: dict[tuple[int, int], float] = {}
    for linea in texto.splitlines()[1:]:
        partes = linea.split()
        if len(partes) != 4 or partes[0] not in TRIMESTRES:
            continue
        trimestre, anio, anomalia = partes[0], int(partes[1]), float(partes[3])
        mes_fin = (TRIMESTRES.index(trimestre) + 1) % 12 + 1
        out[(anio + 1 if trimestre == "NDJ" else anio, mes_fin)] = anomalia
    return out


def traer_oni(http: HttpCache) -> dict[tuple[int, int], float]:
    return leer_oni(http.texto(URL_ONI))
