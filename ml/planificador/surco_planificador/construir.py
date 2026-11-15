"""Etapas `descargar` y `limpiar`: datos públicos de cada punto -> el conjunto limpio."""

import json
import logging
from collections.abc import Sequence
from concurrent.futures import ThreadPoolExecutor

import numpy as np
import numpy.typing as npt

from surco_planificador.caracteristicas import CANALES_HISTORIA
from surco_planificador.config import ConfigPipeline
from surco_planificador.datos import DatosLimpios
from surco_planificador.fuentes.http import HttpCache
from surco_planificador.fuentes.oni import traer_oni
from surco_planificador.fuentes.power import SeriePower, traer_power
from surco_planificador.fuentes.suelo import (
    PROFUNDIDADES,
    PROPIEDADES,
    PerfilSuelo,
    RasterSoilGrids,
    cargar_perfil,
    guardar_perfil,
    media_entre,
)
from surco_planificador.limpieza.calidad import ajuste_elevacion_c, limpiar_valores
from surco_planificador.limpieza.climatologia import sin_fuga
from surco_planificador.limpieza.et0 import (
    hargreaves,
    penman_monteith,
    presion_saturacion,
    radiacion_desde_temperatura,
)
from surco_planificador.puntos import Punto
from surco_planificador.rutas import Rutas

log = logging.getLogger(__name__)
F32 = npt.NDArray[np.float32]
OBLIGATORIOS = ("t2m", "tmax", "tmin", "tdew", "lluvia", "hr", "viento", "et0", "gwetroot")


def http_de(rutas: Rutas) -> HttpCache:
    return HttpCache(rutas.crudo / "http")


def descargar(rutas: Rutas, puntos: Sequence[Punto], cfg: ConfigPipeline) -> None:
    """Todo a la caché; se puede interrumpir y volver a correr."""
    http = http_de(rutas)
    carpeta_suelo = rutas.crudo / "suelo"
    carpeta_suelo.mkdir(parents=True, exist_ok=True)

    def power(p: Punto) -> None:
        traer_power(http, p.lat, p.lon, cfg.primer_dia, cfg.ultimo_dia)
        log.info("POWER %s %s ok", p.id, p.nombre)

    def suelo() -> None:
        lats, lons = [p.lat for p in puntos], [p.lon for p in puntos]
        raster = RasterSoilGrids(rutas.crudo / "soilgrids", (min(lats) - 0.5, max(lats) + 0.5,
                                                             min(lons) - 0.5, max(lons) + 0.5))  # fmt: skip
        for p, perfil in zip(puntos, raster.perfiles([(p.lat, p.lon) for p in puntos], cfg.radio_suelo_km),
                             strict=True):  # fmt: skip
            guardar_perfil(carpeta_suelo / f"{p.id}.json", perfil)
            log.info("SoilGrids %s %s %s", p.id, p.nombre, "ok" if perfil else "SIN PERFIL")

    traer_oni(http)
    with ThreadPoolExecutor(max_workers=5) as pool:
        trabajo_suelo = pool.submit(suelo)
        for t in [pool.submit(power, p) for p in puntos]:
            t.result()
        trabajo_suelo.result()


def canales_clima(serie: SeriePower, p: Punto, cfg: ConfigPipeline, doy: npt.NDArray[np.float64]
                  ) -> tuple[F32, dict[str, object]]:
    v = {k: x.copy() for k, x in serie.valores.items()}
    conteos = limpiar_valores(v, cfg.max_hueco_dias)
    ajuste = ajuste_elevacion_c(p.elevacion_m, serie.elevacion_celda_m, cfg.gradiente_c_por_km)
    t, tmax, tmin = (v[k] + ajuste for k in ("T2M", "T2M_MAX", "T2M_MIN"))
    estimada = ~np.isfinite(v["ALLSKY_SFC_SW_DWN"])  # POWER no tiene radiación 1981-1983
    rad = np.where(estimada, radiacion_desde_temperatura(tmax, tmin, p.lat, doy), v["ALLSKY_SFC_SW_DWN"])
    ea = presion_saturacion(v["T2MDEW"])  # el rocío no se corrige por elevación
    pm = penman_monteith(tmax, tmin, ea, v["WS2M"], rad, p.lat, p.elevacion_m, doy)
    et0 = np.where(estimada, hargreaves(tmax, tmin, p.lat, doy), pm)
    canales = {"t2m": t, "tmax": tmax, "tmin": tmin, "tdew": v["T2MDEW"], "lluvia": v["PRECTOTCORR"],
               "hr": v["RH2M"], "viento": v["WS2M"], "radiacion": rad, "et0": et0,
               "gwetroot": v["GWETROOT"], "radiacion_estimada": estimada.astype(np.float64)}  # fmt: skip
    informe = {**conteos, "elevacion_celda_m": serie.elevacion_celda_m,
               "ajuste_temperatura_c": round(ajuste, 2), "dias_radiacion_estimada": int(estimada.sum())}  # fmt: skip
    return np.stack([canales[c] for c in CANALES_HISTORIA], -1).astype(np.float32), informe


def _arreglo_perfil(perfil: PerfilSuelo) -> npt.NDArray[np.float64]:
    return np.array([[perfil.capas[p][k] for p in PROPIEDADES] for k in range(len(PROFUNDIDADES))])


def limpiar_punto(rutas: Rutas, p: Punto, cfg: ConfigPipeline, anios: range) -> dict[str, object] | str:
    serie = traer_power(http_de(rutas), p.lat, p.lon, cfg.primer_dia, cfg.ultimo_dia)
    if serie.dias[0] != cfg.primer_dia or serie.dias[-1] != cfg.ultimo_dia:
        return "la serie de POWER no cubre el periodo configurado"
    perfil = cargar_perfil(rutas.crudo / "suelo" / f"{p.id}.json")
    if perfil is None:
        return "sin píxel de SoilGrids con todas las capas dentro del radio"
    doy = np.array([d.timetuple().tm_yday for d in serie.dias], dtype=np.float64)
    clima, informe = canales_clima(serie, p, cfg, doy)
    idx = {c: i for i, c in enumerate(CANALES_HISTORIA)}
    valido = np.all(np.isfinite(clima[:, [idx[c] for c in OBLIGATORIOS]]), axis=1)
    enmascarado = 1 - float(valido.mean())
    if enmascarado > cfg.max_fraccion_enmascarada:
        return f"{enmascarado:.1%} de días enmascarados (límite {cfg.max_fraccion_enmascarada:.0%})"
    arr = _arreglo_perfil(perfil)
    sitio = np.concatenate([[p.lat, p.lon, p.elevacion_m, p.elevacion_m - serie.elevacion_celda_m],
                            media_entre(arr, 0, 30), media_entre(arr, 30, 60)]).astype(np.float32)  # fmt: skip
    series = {k: clima[:, idx[c]].astype(np.float64)
              for k, c in (("t", "t2m"), ("lluvia", "lluvia"), ("tmax", "tmax"), ("tmin", "tmin"), ("et0", "et0"))}  # fmt: skip
    clim = sin_fuga(series, cfg.primer_dia, cfg.anios_base, anios, cfg.ventana_suavizado)
    return {"clima": np.where(np.isfinite(clima), clima, 0.0).astype(np.float32), "valido": valido,
            "sitio": sitio, "perfil": arr.astype(np.float32), "clim": clim,
            "informe": {**informe, "fraccion_enmascarada": round(enmascarado, 4)}}  # fmt: skip


def anios_etiqueta(cfg: ConfigPipeline) -> range:
    return range(cfg.primer_dia.year + 1, cfg.ultimo_dia.year + 1)


def limpiar(rutas: Rutas, puntos: Sequence[Punto], cfg: ConfigPipeline) -> tuple[DatosLimpios, list[Punto]]:
    anios = anios_etiqueta(cfg)
    quedan: list[tuple[Punto, dict[str, object]]] = []
    informe: dict[str, object] = {}
    for p in puntos:
        r = limpiar_punto(rutas, p, cfg, anios)
        if isinstance(r, str):
            log.warning("excluido %s %s: %s", p.id, p.nombre, r)
            informe[p.id] = {"nombre": p.nombre, "excluido": r}
            continue
        quedan.append((p, r))
        informe[p.id] = {"nombre": p.nombre, **r["informe"]}  # type: ignore[dict-item]
    rutas.limpio.mkdir(parents=True, exist_ok=True)
    (rutas.limpio / "informe_calidad.json").write_text(json.dumps(informe, indent=1, ensure_ascii=False),
                                                       encoding="utf-8")  # fmt: skip
    oni = traer_oni(http_de(rutas))
    meses = sorted(oni)
    log.info("limpiar: %d puntos quedan, %d excluidos", len(quedan), len(puntos) - len(quedan))

    def pila(clave: str) -> npt.NDArray[np.generic]:
        return np.stack([r[clave] for _, r in quedan])  # type: ignore[misc]

    datos = DatosLimpios(
        ids=np.array([p.id for p, _ in quedan]), departamentos=np.array([p.departamento for p, _ in quedan]),
        lat=np.array([p.lat for p, _ in quedan], np.float32), lon=np.array([p.lon for p, _ in quedan], np.float32),
        elevacion=np.array([p.elevacion_m for p, _ in quedan], np.float32), dia0=cfg.primer_dia,
        clima=pila("clima"), valido=pila("valido"), sitio=pila("sitio"), perfil_suelo=pila("perfil"),
        oni_meses=np.array([a * 12 + m - 1 for a, m in meses], np.int32),
        oni_valores=np.array([oni[k] for k in meses], np.float32),
        anios_clim=np.array(list(anios), np.int32), clim=pila("clim"),
    )  # type: ignore[arg-type]  # fmt: skip
    return datos, [p for p, _ in quedan]
