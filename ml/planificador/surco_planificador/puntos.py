"""Puntos de entrenamiento: un centroide municipal (DIVIPOLA) por celda de NASA POWER.

Dos puntos en la misma celda comparten el mismo clima: duplicarían muestras sin aportar
información. Se conserva el municipio más cercano al centro de la celda.
"""

import csv
import json
import math
import random
from dataclasses import asdict, dataclass, fields
from pathlib import Path

from surco_planificador.fuentes.elevacion import traer_elevaciones
from surco_planificador.fuentes.http import HttpCache

CELDA_POWER = (0.5, 0.625)  # grilla MERRA-2 detrás de la meteorología de POWER


@dataclass(frozen=True)
class Punto:
    id: str
    municipio: str
    departamento: str
    nombre: str
    lat: float
    lon: float
    elevacion_m: float


def celda_power(lat: float, lon: float) -> tuple[int, int]:
    return math.floor((lat + 90) / CELDA_POWER[0]), math.floor((lon + 180) / CELDA_POWER[1])


def _distancia_al_centro(lat: float, lon: float) -> float:
    i, j = celda_power(lat, lon)
    centro = (-90 + (i + 0.5) * CELDA_POWER[0], -180 + (j + 0.5) * CELDA_POWER[1])
    return math.hypot(lat - centro[0], lon - centro[1])


def seleccionar_puntos(divipola: Path, http: HttpCache, max_puntos: int, semilla: int) -> list[Punto]:
    datos = json.loads(divipola.read_text(encoding="utf-8"))
    mejores: dict[tuple[int, int], dict[str, object]] = {}
    for dep in datos["departamentos"]:
        for m in dep["municipios"]:
            m = {**m, "departamento": dep["codigo"]}
            celda = celda_power(m["lat"], m["lon"])
            if celda not in mejores or _distancia_al_centro(m["lat"], m["lon"]) < _distancia_al_centro(
                mejores[celda]["lat"], mejores[celda]["lon"]  # type: ignore[arg-type]
            ):
                mejores[celda] = m
    elegidos = sorted(mejores.values(), key=lambda m: str(m["codigo"]))
    if len(elegidos) > max_puntos:
        elegidos = sorted(random.Random(semilla).sample(elegidos, max_puntos), key=lambda m: str(m["codigo"]))
    elevaciones = traer_elevaciones(http, [(float(m["lat"]), float(m["lon"])) for m in elegidos])  # type: ignore[arg-type]
    return [Punto(f"p{i:03d}", str(m["codigo"]), str(m["departamento"]), str(m["nombre"]),
                  float(m["lat"]), float(m["lon"]), e)  # type: ignore[arg-type]
            for i, (m, e) in enumerate(zip(elegidos, elevaciones, strict=True))]  # fmt: skip


def escribir_puntos(ruta: Path, puntos: list[Punto]) -> None:
    with ruta.open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=[x.name for x in fields(Punto)])
        w.writeheader()
        w.writerows(asdict(p) for p in puntos)


def leer_puntos(ruta: Path) -> list[Punto]:
    with ruta.open(encoding="utf-8") as f:
        return [Punto(r["id"], r["municipio"], r["departamento"], r["nombre"], float(r["lat"]),
                      float(r["lon"]), float(r["elevacion_m"])) for r in csv.DictReader(f)]  # fmt: skip
