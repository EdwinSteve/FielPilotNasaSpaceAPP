"""Coordenadas del terreno + fecha de siembra -> los ciclos productivos recomendados.

Trae todo lo del terreno de las mismas fuentes públicas del entrenamiento y lo procesa con las
mismas funciones, para que la red reciba datos con la misma forma y estadística:

1. Elevación (Open-Meteo), clima diario de NASA POWER desde 1981 y suelo de SoilGrids en el punto.
2. ONI de NOAA y el pronóstico estacional del terreno. Con `fuente: analogos` (pipeline.yaml) es
   el conjunto de años análogos del propio terreno, elegidos igual que en el entrenamiento.
3. La red del bundle exportado puntúa los cultivos (inferencia.Planificador).

Uso:  python -m surco_planificador.servicio --lat 4.60 --lon -74.08 --siembra 2026-12-10
"""

import argparse
import json
import logging
from dataclasses import asdict
from datetime import date, timedelta
from pathlib import Path

import numpy as np
import numpy.typing as npt

from surco_planificador.caracteristicas import CANALES_HISTORIA, oni_conocido
from surco_planificador.catalogo import cargar_fichas
from surco_planificador.config import ConfigPipeline, ConfigPronostico, cargar_pipeline
from surco_planificador.construir import _arreglo_perfil, canales_clima
from surco_planificador.fuentes.c3s import CANALES_CRUDOS
from surco_planificador.fuentes.elevacion import traer_elevaciones
from surco_planificador.fuentes.http import HttpCache
from surco_planificador.fuentes.oni import traer_oni
from surco_planificador.fuentes.power import traer_power
from surco_planificador.fuentes.suelo import PROPIEDADES, RasterSoilGrids, agua_disponible_mm, media_entre
from surco_planificador.inferencia import Lote, Planificador, Recomendacion
from surco_planificador.limpieza.climatologia import servida
from surco_planificador.puntos import Punto
from surco_planificador.rutas import Rutas

log = logging.getLogger(__name__)
F32 = npt.NDArray[np.float32]
RETRASO_POWER_DIAS = 7  # POWER publica con unos días de retraso; lo último es provisional


def inicio_para(siembra: date, dia_disponible: int) -> date:
    """El inicio de pronóstico que el agricultor ya tendría el día de siembra (como en el entrenamiento)."""
    if siembra.day >= dia_disponible:
        return siembra.replace(day=1)
    return (siembra.replace(day=1) - timedelta(days=1)).replace(day=1)


def elegir_analogos(oni: dict[tuple[int, int], float], inicio: date, dia0: date, n_dias: int,
                    miembros: int, dias_lead: int) -> list[int]:
    """Índices de arranque de los `miembros` años con el ONI más parecido al conocido en el inicio.
    Misma regla que pronostico.analogos_todo: fuera el año del inicio y sus vecinos."""
    ref = oni_conocido(oni, inicio)
    candidatos: list[tuple[float, int]] = []
    for y in range(dia0.year, dia0.year + n_dias // 365 + 1):
        if abs(y - inicio.year) <= 1:
            continue
        s = (date(y, inicio.month, 1) - dia0).days
        if s < 0 or s + dias_lead > n_dias:
            continue
        try:
            k = oni_conocido(oni, date(y, inicio.month, 1))
        except KeyError:
            continue
        candidatos.append((abs(k[0] - ref[0]) + 0.5 * abs(k[1] - ref[1]), s))
    if len(candidatos) < miembros:
        raise ValueError(f"solo {len(candidatos)} años análogos disponibles; se necesitan {miembros}")
    return [s for _, s in sorted(candidatos)[:miembros]]


def oni_al(oni: dict[tuple[int, int], float], fecha: date) -> tuple[float, float]:
    """ONI conocido a la fecha; si la fecha es futura, el último publicado."""
    while True:
        try:
            return oni_conocido(oni, fecha)
        except KeyError:
            fecha = (fecha.replace(day=1) - timedelta(days=1)).replace(day=1)


def preparar_lote(lat: float, lon: float, siembra: date, cfg: ConfigPipeline, fichas: list, cache: Path,
                  riego: bool = False) -> tuple[Lote, F32, date, dict[tuple[int, int], float]]:
    """El Lote de inferencia y la serie diaria completa del terreno [D, CANALES_HISTORIA]."""
    http = HttpCache(cache / "http")
    elevacion = float(traer_elevaciones(http, [(lat, lon)])[0])
    hasta = min(siembra - timedelta(days=1), date.today() - timedelta(days=RETRASO_POWER_DIAS))
    serie = traer_power(http, lat, lon, cfg.primer_dia, hasta)
    punto = Punto(id="lote", municipio="", departamento="", nombre="lote", lat=lat, lon=lon, elevacion_m=elevacion)
    doy = np.array([d.timetuple().tm_yday for d in serie.dias], dtype=np.float64)
    clima, _ = canales_clima(serie, punto, cfg, doy)
    clima = np.where(np.isfinite(clima), clima, 0.0).astype(np.float32)

    perfil = RasterSoilGrids(cache / "soilgrids", (lat - 0.5, lat + 0.5, lon - 0.5, lon + 0.5)).perfiles(
        [(lat, lon)], cfg.radio_suelo_km)[0]  # fmt: skip
    if perfil is None:
        raise ValueError(f"SoilGrids no tiene un perfil completo a menos de {cfg.radio_suelo_km} km de ({lat}, {lon})")
    arr = _arreglo_perfil(perfil)
    sitio = np.concatenate([[lat, lon, elevacion, elevacion - serie.elevacion_celda_m],
                            media_entre(arr, 0, 30), media_entre(arr, 30, 60)]).astype(np.float32)  # fmt: skip
    idx = {c: i for i, c in enumerate(CANALES_HISTORIA)}
    series = {k: clima[:, idx[c]].astype(np.float64)
              for k, c in (("t", "t2m"), ("lluvia", "lluvia"), ("tmax", "tmax"), ("tmin", "tmin"), ("et0", "et0"))}  # fmt: skip
    lote = Lote(lat=lat, lon=lon, elevacion_m=elevacion, sitio=sitio, historia=clima[-365:],
                clim=servida(series, cfg.primer_dia, cfg.anios_base, cfg.ventana_suavizado),
                ph_suelo=float(media_entre(arr, 0, 30)[PROPIEDADES.index("phh2o")]),
                taw_por_cultivo={f.id: float(agua_disponible_mm(arr, f.zr)) for f in fichas}, riego=riego)  # fmt: skip
    return lote, clima, cfg.primer_dia, traer_oni(http)


def pronostico_analogos(clima: F32, dia0: date, oni: dict[tuple[int, int], float], inicio: date,
                        pc: ConfigPronostico) -> F32:
    """[miembros, dias_lead, CANALES_CRUDOS] desde la propia serie del terreno."""
    canales = [CANALES_HISTORIA.index(c) for c in CANALES_CRUDOS]
    arranques = elegir_analogos(oni, inicio, dia0, clima.shape[0], pc.miembros, pc.dias_lead)
    return np.stack([clima[s:s + pc.dias_lead][:, canales] for s in arranques]).astype(np.float32)


def ultimo_bundle(rutas: Rutas) -> Path:
    bundles = sorted(p for p in rutas.modelos.glob("planificador-*") if (p / "model.onnx").exists())
    if not bundles:
        raise FileNotFoundError(f"no hay bundles exportados en {rutas.modelos}")
    return bundles[-1]


def recomendar_por_coordenadas(lat: float, lon: float, siembra: date, riego: bool = False,
                               bundle: Path | None = None, rutas: Rutas | None = None,
                               crudo_seas5: F32 | None = None) -> dict[str, object]:
    """Todo el flujo para un terreno. `crudo_seas5` [M, T, 6] solo para un bundle entrenado con SEAS5."""
    rutas = rutas or Rutas.por_defecto()
    cfg = cargar_pipeline(rutas.configs / "pipeline.yaml")
    fichas, _, _ = cargar_fichas(rutas.catalogo_json, rutas.configs / "ky_fao33.yaml", cfg.max_ciclo_dias, cfg.excluir)
    bundle = bundle or ultimo_bundle(rutas)
    plan = Planificador(bundle, fichas)
    lote, clima, dia0, oni = preparar_lote(lat, lon, siembra, cfg, plan.fichas, rutas.crudo / "servicio", riego)
    inicio = inicio_para(siembra, cfg.pronostico.dia_disponible)
    if cfg.pronostico.fuente == "analogos":
        crudo = pronostico_analogos(clima, dia0, oni, inicio, cfg.pronostico)
        gradiente = 0.0  # los análogos ya vienen del terreno y a su elevación
    else:
        if crudo_seas5 is None:
            raise ValueError("este bundle se entrenó con SEAS5: pase el pronóstico SEAS5 interpolado al lote")
        crudo, gradiente = crudo_seas5, cfg.gradiente_c_por_km
    diario = plan.corregir(crudo, inicio, lote, gradiente=gradiente)
    valor, tendencia = oni_al(oni, siembra)
    recs: list[Recomendacion] = plan.recomendar(lote, diario, inicio, siembra, valor, tendencia)
    return {
        "lote": {"lat": lat, "lon": lon, "elevacion_m": round(lote.elevacion_m, 1), "ph_suelo": round(lote.ph_suelo, 2)},
        "siembra": siembra.isoformat(), "inicio_pronostico": inicio.isoformat(),
        "pronostico": cfg.pronostico.fuente, "oni": round(valor, 2), "oni_tendencia_3m": round(tendencia, 2),
        "bundle": bundle.name, "riego": riego,
        "recomendaciones": [{k: (round(v, 3) if isinstance(v, float) else v) for k, v in asdict(r).items()}
                            for r in recs],  # fmt: skip
    }


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--lat", type=float, required=True)
    ap.add_argument("--lon", type=float, required=True)
    ap.add_argument("--siembra", type=date.fromisoformat, default=date.today())
    ap.add_argument("--riego", action="store_true")
    ap.add_argument("--bundle", type=Path)
    args = ap.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
    print(json.dumps(recomendar_por_coordenadas(args.lat, args.lon, args.siembra, args.riego, args.bundle),
                     indent=1, ensure_ascii=False))  # fmt: skip


if __name__ == "__main__":
    main()
