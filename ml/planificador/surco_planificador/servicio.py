"""Coordenadas del terreno + fecha de siembra -> los ciclos productivos recomendados.

Trae todo lo del terreno de las mismas fuentes públicas del entrenamiento y lo procesa con las
mismas funciones, para que la red reciba datos con la misma forma y estadística:

1. Elevación (Open-Meteo), clima diario de NASA POWER desde 1981 y suelo de SoilGrids en el punto.
2. ONI de NOAA y el pronóstico estacional del terreno. Con `fuente: analogos` (pipeline.yaml) es
   el conjunto de años análogos del propio terreno, elegidos igual que en el entrenamiento.
3. Bf (el oráculo sobre cada miembro del pronóstico) puntúa los cultivos; la red del bundle no le
   gana (MODELO.md) y queda como opción con SURCO_MOTOR=red (inferencia.Planificador).
4. Para las temporadas siguientes de una rotación (fuera de los 175 días del pronóstico): la
   frecuencia de éxito de cada cultivo en los años pasados del propio terreno (`historico`).

Uso:  python -m surco_planificador.servicio --lat 4.60 --lon -74.08 --siembra 2026-12-10
"""

import argparse
import json
import logging
import os
import urllib.error
from collections.abc import Callable
from dataclasses import asdict
from datetime import date, timedelta
from pathlib import Path

import numpy as np
import numpy.typing as npt

from surco_planificador.caracteristicas import CANALES_HISTORIA, oni_conocido
from surco_planificador.catalogo import cargar_documentos, cargar_fichas
from surco_planificador.config import ConfigPipeline, ConfigPronostico, cargar_pipeline
from surco_planificador.construir import _arreglo_perfil, canales_clima
from surco_planificador.errores import FuenteCaida, TerrenoSinSuelo
from surco_planificador.fuentes.c3s import CANALES_CRUDOS
from surco_planificador.fuentes.elevacion import traer_elevaciones
from surco_planificador.fuentes.http import HttpCache
from surco_planificador.fuentes.oni import traer_oni
from surco_planificador.fuentes.power import traer_power
from surco_planificador.fuentes.suelo import (
    PROPIEDADES,
    RasterSoilGrids,
    agua_disponible_mm,
    media_entre,
)
from surco_planificador.inferencia import MOTOR_BF, MOTORES, Lote, Planificador, Recomendacion
from surco_planificador.limpieza.climatologia import INDICE, indice_doy, servida
from surco_planificador.oraculo import CicloClima
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
    elevacion = float(desde_fuente("Open-Meteo (elevación)", lambda: traer_elevaciones(http, [(lat, lon)]))[0])
    hasta = min(siembra - timedelta(days=1), date.today() - timedelta(days=RETRASO_POWER_DIAS))
    serie = desde_fuente("NASA POWER", lambda: traer_power(http, lat, lon, cfg.primer_dia, hasta))
    punto = Punto(id="lote", municipio="", departamento="", nombre="lote", lat=lat, lon=lon, elevacion_m=elevacion)
    doy = np.array([d.timetuple().tm_yday for d in serie.dias], dtype=np.float64)
    clima, _ = canales_clima(serie, punto, cfg, doy)
    clima = np.where(np.isfinite(clima), clima, 0.0).astype(np.float32)

    raster = RasterSoilGrids(cache / "soilgrids", (lat - 0.5, lat + 0.5, lon - 0.5, lon + 0.5))
    perfil = desde_fuente("SoilGrids", lambda: raster.perfiles([(lat, lon)], cfg.radio_suelo_km))[0]
    if perfil is None:
        raise TerrenoSinSuelo(f"SoilGrids no tiene un perfil de suelo completo a menos de {cfg.radio_suelo_km} km "
                              f"de ({lat}, {lon})")  # fmt: skip
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
    return lote, clima, cfg.primer_dia, desde_fuente("NOAA (ONI)", lambda: traer_oni(http))


ERRORES_DE_RED = (urllib.error.URLError, TimeoutError, ConnectionError, json.JSONDecodeError, OSError)


def desde_fuente[T](fuente: str, traer: Callable[[], T]) -> T:
    """Corre `traer`; si la fuente externa falla (red, HTTP o respuesta ilegible), FuenteCaida(fuente).
    HttpCache ya reintentó con espera: aquí la falla es definitiva para esta consulta."""
    try:
        return traer()
    except ERRORES_DE_RED as e:  # urllib.error.HTTPError es URLError
        raise FuenteCaida(fuente, str(e) or type(e).__name__) from e


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


DIAS_PANORAMA = 30
BLOQUES_PANORAMA = 6  # 180 días: lo que cubre el pronóstico desde cualquier fecha de siembra


def _redondear(r: Recomendacion) -> dict[str, object]:
    return {k: (round(v, 3) if isinstance(v, float) else v) for k, v in asdict(r).items()}


def fechas_candidatas(hoy: date, n: int = 6, paso: int = 5) -> list[date]:
    """Las siembras que se comparan: desde mañana, cada `paso` días, durante el próximo mes."""
    return [hoy + timedelta(days=1 + paso * k) for k in range(n)]


# Temporadas siguientes de una rotación: fechas de siembra cada 15 días, de 30 a 540 días desde hoy
HISTORICO_DESDE_DIAS = 30
HISTORICO_PASO_DIAS = 15
HISTORICO_FECHAS = 35
PREVIO_DIAS = 30  # mes antes de sembrar con el que arranca el balance hídrico (como en _priors)
DESCRIPCION_MOTOR = {
    "bf": "Oráculo agronómico sobre cada miembro del pronóstico (baseline Bf); la red no le gana (MODELO.md).",
    "red": "Red neuronal del bundle (no adoptada: no le gana a Bf en pinball, MODELO.md).",
}


def decimal(x: float) -> str:
    """5.5 -> "5,5" (coma decimal, como en la app)."""
    return f"{x:.1f}".replace(".", ",")


def kg_fijacion(docs: list[dict[str, object]]) -> dict[str, int | None]:
    """Fijación biológica de nitrógeno por cultivo (kg N/ha, punto medio del rango del catálogo)."""
    kg: dict[str, int | None] = {}
    for d in docs:
        rango = (d.get("nitrogeno") or {}).get("fijacion_biologica_kg_ha")  # type: ignore[union-attr]
        kg[str(d["_id"])] = round((rango["min"] + rango["max"]) / 2) if rango else None  # type: ignore[index]
    return kg


class Motor:
    """El modelo cargado una vez (bundle ONNX, catálogo, configuración) para servir muchas consultas.

    `motor`: "bf" (por defecto: el baseline Bf, que la red no supera) o "red". Variable SURCO_MOTOR."""

    def __init__(self, rutas: Rutas | None = None, bundle: Path | None = None, motor: str | None = None) -> None:
        self.rutas = rutas or Rutas.por_defecto()
        self.motor = motor or os.environ.get("SURCO_MOTOR", MOTOR_BF)
        if self.motor not in MOTORES:
            raise ValueError(f"SURCO_MOTOR={self.motor}: use {' o '.join(MOTORES)}")
        self.cfg = cargar_pipeline(self.rutas.configs / "pipeline.yaml")
        fichas, _, _ = cargar_fichas(self.rutas.catalogo_json, self.rutas.configs / "ky_fao33.yaml",
                                     self.cfg.max_ciclo_dias, self.cfg.excluir)  # fmt: skip
        self.bundle = bundle or ultimo_bundle(self.rutas)
        self.plan = Planificador(self.bundle, fichas)
        self.fichas = {f.id: f for f in self.plan.fichas}
        docs, _ = cargar_documentos(self.rutas.catalogo_json)
        self.fijacion = kg_fijacion(docs)

    def _diario(self, clima: F32, dia0: date, oni: dict[tuple[int, int], float], inicio: date, lote: Lote,
                crudo_seas5: F32 | None) -> F32:
        if self.cfg.pronostico.fuente == "analogos":
            crudo = pronostico_analogos(clima, dia0, oni, inicio, self.cfg.pronostico)
            return self.plan.corregir(crudo, inicio, lote, gradiente=0.0)  # análogos: ya a la elevación del lote
        if crudo_seas5 is None:
            raise ValueError("este bundle se entrenó con SEAS5: pase el pronóstico SEAS5 interpolado al lote")
        return self.plan.corregir(crudo_seas5, inicio, lote, gradiente=self.cfg.gradiente_c_por_km)

    def _apto(self, cultivo: str, elevacion: float, ph: float) -> str | None:
        """Razón por la que un cultivo no es apto en el lote (filtros duros de la ficha); None si lo es."""
        f = self.fichas[cultivo]
        bajo, alto = f.altitud
        if (bajo or alto) and not (bajo <= elevacion <= alto):
            return f"Se cultiva entre {bajo:.0f} y {alto:.0f} m de altitud; su terreno está a {elevacion:.0f} m."
        if not (f.ph.a <= ph <= f.ph.d):
            return f"Necesita un suelo con pH entre {decimal(f.ph.a)} y {decimal(f.ph.d)}; el de su terreno es {decimal(ph)}."
        return None

    def _ficha(self, cultivo: str) -> dict[str, object]:
        """Lo que la rotación necesita del catálogo: familia botánica y nitrógeno que deja."""
        f = self.fichas[cultivo]
        return {"familia": f.familia, "fija_nitrogeno": f.fija_nitrogeno,
                "fijacion_nitrogeno_kg_ha": self.fijacion.get(cultivo) if f.fija_nitrogeno else None}  # fmt: skip

    def _panorama(self, diario: F32, inicio: date, desde: date, lote: Lote) -> list[dict[str, object]]:
        """Lluvia y temperatura esperadas por bloques de 30 días desde `desde`, frente a lo normal."""
        canales = {c: i for i, c in enumerate(("t2m", "tmax", "tmin", "lluvia", "et0", "hr"))}
        o = (desde - inicio).days
        bloques = []
        for b in range(BLOQUES_PANORAMA):
            a, z = o + b * DIAS_PANORAMA, o + (b + 1) * DIAS_PANORAMA
            if z > diario.shape[1]:
                break
            lluvia = diario[:, a:z, canales["lluvia"]].sum(axis=1)  # [M]
            doy = [indice_doy(inicio + timedelta(days=t)) for t in range(a, z)]
            normal = float(lote.clim[doy, INDICE["lluvia_media"]].sum())
            p10, p50, p90 = (float(x) for x in np.percentile(lluvia, [10, 50, 90]))
            bloques.append({
                "desde": (inicio + timedelta(days=a)).isoformat(), "hasta": (inicio + timedelta(days=z - 1)).isoformat(),
                "lluvia_mm_p10": round(p10), "lluvia_mm_p50": round(p50), "lluvia_mm_p90": round(p90),
                "lluvia_normal_mm": round(normal),
                "anomalia_lluvia_pct": round(100 * (p50 - normal) / normal) if normal > 1 else 0,
                "tmax_c": round(float(diario[:, a:z, canales["tmax"]].mean()), 1),
                "tmin_c": round(float(diario[:, a:z, canales["tmin"]].mean()), 1),
                "tmax_normal_c": round(float(lote.clim[doy, INDICE["tmax_media"]].mean()), 1),
            })  # fmt: skip
        return bloques

    def historico(self, lote: Lote, clima: F32, dia0: date, hoy: date) -> dict[str, object]:
        """Frecuencia con la que cada cultivo habría salido bien en los años pasados del propio terreno,
        sembrando cada 15 días entre 30 y 540 días desde hoy: el oráculo sobre el clima que ocurrió
        (como el baseline B1). Es lo que se sabe de las temporadas 2 y 3 de una rotación, que caen
        fuera de los 175 días que cubre el pronóstico."""
        fechas = [hoy + timedelta(days=HISTORICO_DESDE_DIAS + k * HISTORICO_PASO_DIAS) for k in range(HISTORICO_FECHAS)]
        idx = [CANALES_HISTORIA.index(c) for c in ("t2m", "tmax", "tmin", "lluvia", "et0")]
        anio0, anio1 = dia0.year, dia0.year + clima.shape[0] // 366
        cultivos: dict[str, dict[str, list[float | None]]] = {}
        anios_usados: set[int] = set()
        for f, orc in zip(self.plan.fichas, self.plan.oraculos, strict=True):
            L = f.ciclo_dias
            inicios, de_fecha = [], []
            for i, fecha in enumerate(fechas):
                for y in range(anio0, anio1 + 1):
                    try:
                        s = (fecha.replace(year=y) - dia0).days
                    except ValueError:  # 29 de febrero en un año no bisiesto
                        s = (fecha.replace(year=y, day=28) - dia0).days
                    if s - PREVIO_DIAS >= 0 and s + L <= clima.shape[0]:
                        inicios.append(s)
                        de_fecha.append(i)
                        anios_usados.add(y)
            inicios_np, de_fecha_np = np.array(inicios), np.array(de_fecha)
            ciclo = clima[inicios_np[:, None] + np.arange(L)][..., idx].astype(np.float64)  # [N, L, 5]
            previo = clima[inicios_np[:, None] + np.arange(-PREVIO_DIAS, 0)].astype(np.float64)
            r = orc.simular(CicloClima(*(ciclo[..., k] for k in range(5)), previo[..., idx[3]], previo[..., idx[4]]),
                            lote.taw_por_cultivo[f.id], lote.ph_suelo)  # [N, 6]  # fmt: skip
            col: dict[str, list[float | None]] = {"prob_exito": [], "rend_p50": [], "riego_mm_p50": [],
                                                  "rend_con_riego_p50": []}  # fmt: skip
            for i in range(len(fechas)):
                m = de_fecha_np == i
                vacio = not m.any()
                col["prob_exito"].append(None if vacio else round(float(r[m, 1].mean()), 3))
                col["rend_p50"].append(None if vacio else round(float(np.median(r[m, 0])), 3))
                col["riego_mm_p50"].append(None if vacio else round(float(np.median(r[m, 2])), 1))
                col["rend_con_riego_p50"].append(None if vacio else round(float(np.median(r[m, 5])), 3))
            cultivos[f.id] = col
        return {"fuente": f"NASA POWER {min(anios_usados)}-{max(anios_usados)} (años pasados del terreno)",
                "fechas": [x.isoformat() for x in fechas], "cultivos": cultivos}  # fmt: skip

    def planificar(self, lat: float, lon: float, riego: bool = False, hoy: date | None = None,
                   siembras: list[date] | None = None, crudo_seas5: F32 | None = None) -> dict[str, object]:
        """El mejor ciclo productivo para el terreno: compara cultivos y fechas de siembra del próximo mes."""
        hoy = hoy or date.today()
        siembras = siembras or fechas_candidatas(hoy)
        lote, clima, dia0, oni = preparar_lote(lat, lon, max(siembras), self.cfg, self.plan.fichas,
                                               self.rutas.crudo / "servicio", riego)  # fmt: skip
        diarios: dict[date, F32] = {}
        opciones = []
        for siembra in siembras:
            inicio = inicio_para(siembra, self.cfg.pronostico.dia_disponible)
            if inicio not in diarios:
                diarios[inicio] = self._diario(clima, dia0, oni, inicio, lote, crudo_seas5)
            valor, tendencia = oni_al(oni, siembra)
            recs = self.plan.recomendar(lote, diarios[inicio], inicio, siembra, valor, tendencia, self.motor)
            cultivos = []
            for r in recs:
                razon = self._apto(r.cultivo, lote.elevacion_m, lote.ph_suelo)
                cultivos.append({**_redondear(r), "nombre": self.fichas[r.cultivo].nombre,
                                 "ciclo_dias": self.fichas[r.cultivo].ciclo_dias,
                                 "cosecha": (siembra + timedelta(days=self.fichas[r.cultivo].ciclo_dias)).isoformat(),
                                 "apto": razon is None, "razon_no_apto": razon, **self._ficha(r.cultivo)})  # fmt: skip
            cultivos.sort(key=lambda c: not c["apto"])  # estable: los aptos primero, en el orden del motor
            opciones.append({"siembra": siembra.isoformat(), "inicio_pronostico": inicio.isoformat(),
                             "cultivos": cultivos})  # fmt: skip

        clave = "rend_con_riego_p50" if riego else "rend_p50"
        aptos = [(o["siembra"], c) for o in opciones for c in o["cultivos"] if c["apto"]]
        mejor = max(aptos, key=lambda x: x[1][clave], default=None)
        primer_inicio = inicio_para(siembras[0], self.cfg.pronostico.dia_disponible)
        valor, tendencia = oni_al(oni, hoy)
        return {
            "lote": {"lat": lat, "lon": lon, "elevacion_m": round(lote.elevacion_m, 1),
                     "ph_suelo": round(lote.ph_suelo, 2), "riego": riego},
            "generado": hoy.isoformat(),
            # oni_tendencia: cambio del ONI en 3 meses (sin dígitos en la clave: el backend mapea snake_case)
            "pronostico": {"fuente": self.cfg.pronostico.fuente, "oni": round(valor, 2),
                           "oni_tendencia": round(tendencia, 2), "fase_enso": fase_enso(valor),
                           "panorama": self._panorama(diarios[primer_inicio], primer_inicio, siembras[0], lote)},
            "mejor": None if mejor is None else {"siembra": mejor[0], "cultivo": mejor[1]},
            "opciones": opciones,
            "historico": self.historico(lote, clima, dia0, hoy),
            "modelo": self.bundle.name,
            "motor": {"id": self.motor, "descripcion": DESCRIPCION_MOTOR[self.motor]},
        }  # fmt: skip


def fase_enso(oni: float) -> str:
    return "nino" if oni >= 0.5 else "nina" if oni <= -0.5 else "neutral"


def recomendar_por_coordenadas(lat: float, lon: float, siembra: date, riego: bool = False,
                               bundle: Path | None = None, rutas: Rutas | None = None,
                               crudo_seas5: F32 | None = None) -> dict[str, object]:
    """Una sola fecha de siembra (compatibilidad con la primera versión del servicio)."""
    motor = Motor(rutas, bundle)
    plan = motor.planificar(lat, lon, riego, hoy=siembra - timedelta(days=1), siembras=[siembra],
                            crudo_seas5=crudo_seas5)  # fmt: skip
    opcion = plan["opciones"][0]  # type: ignore[index]
    return {"lote": plan["lote"], "siembra": opcion["siembra"], "inicio_pronostico": opcion["inicio_pronostico"],
            "pronostico": plan["pronostico"]["fuente"], "oni": plan["pronostico"]["oni"],  # type: ignore[index]
            "oni_tendencia_3m": plan["pronostico"]["oni_tendencia"], "bundle": plan["modelo"],  # type: ignore[index]
            "riego": riego, "motor": plan["motor"], "recomendaciones": opcion["cultivos"]}  # fmt: skip


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--lat", type=float, required=True)
    ap.add_argument("--lon", type=float, required=True)
    ap.add_argument("--siembra", type=date.fromisoformat, help="una sola fecha; sin ella compara el próximo mes")
    ap.add_argument("--riego", action="store_true")
    ap.add_argument("--bundle", type=Path)
    args = ap.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
    if args.siembra:
        salida = recomendar_por_coordenadas(args.lat, args.lon, args.siembra, args.riego, args.bundle)
    else:
        salida = Motor(bundle=args.bundle).planificar(args.lat, args.lon, args.riego)
    print(json.dumps(salida, indent=1, ensure_ascii=False))


if __name__ == "__main__":
    main()
