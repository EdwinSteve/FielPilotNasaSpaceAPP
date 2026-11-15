"""Fichas de cultivo desde el catálogo surco_agro (MongoDB o su JSON versionado).

El cultivo entra a la red como su ficha (temperaturas, lluvias, Kc, raíz, pH... por fase), no como
un identificador: si un agrónomo corrige un valor, la predicción cambia sin reentrenar desde cero,
y un cultivo nuevo con ficha completa se puede puntuar (con la cautela de que no se vio al entrenar).
"""

import hashlib
import json
import logging
import os
from collections.abc import Sequence
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np
import numpy.typing as npt

from surco_planificador.config import cargar_yaml

log = logging.getLogger(__name__)
F32 = npt.NDArray[np.float32]
MAX_FASES = 5
TOLERANCIA = {"baja": 0.0, "media": 1.0, "alta": 2.0}


@dataclass(frozen=True)
class Trapecio:
    """min_absoluto, optimo_min, optimo_max, max_absoluto."""

    a: float
    b: float
    c: float
    d: float

    @classmethod
    def de(cls, t: dict[str, Any]) -> "Trapecio":
        return cls(float(t["min_absoluto"]), float(t["optimo_min"]),
                   float(t["optimo_max"]), float(t["max_absoluto"]))

    def escalado(self, factor: float) -> "Trapecio":
        return Trapecio(self.a * factor, self.b * factor, self.c * factor, self.d * factor)

    def tupla(self) -> tuple[float, float, float, float]:
        return (self.a, self.b, self.c, self.d)


@dataclass(frozen=True)
class Fase:
    codigo: str
    fraccion: float
    kc: float | None
    p: float | None
    temperatura: Trapecio
    t_critica: float | None
    lluvia: Trapecio  # mm acumulados en la fase
    exceso_perjudicial: bool


@dataclass(frozen=True)
class FichaCultivo:
    id: str
    nombre: str
    familia: str
    tipo_ciclo: str
    ciclo_dias: int
    fases: tuple[Fase, ...]
    temperatura: Trapecio
    agua_dia: Trapecio  # mm/día
    hr: tuple[float, float]
    viento: tuple[float, float]  # óptimo máximo, máximo tolerable (m/s a 2 m)
    kc: tuple[float, float, float] | None
    zr: float
    p: float | None
    ky: float
    ky_metodo: str
    ph: Trapecio
    nitrogeno: Trapecio | None
    altitud: tuple[float, float]
    t_base: float
    tol_sequia: float
    tol_encharcamiento: float
    fija_nitrogeno: bool

    @property
    def balance_hidrico(self) -> bool:
        """Con Kc y p se corre FAO-56; sin ellos el agua se evalúa con los trapecios de lluvia."""
        return self.kc is not None and self.p is not None

    def limites_fases(self) -> list[tuple[int, int]]:
        cortes = np.round(np.cumsum([f.fraccion for f in self.fases]) * self.ciclo_dias).astype(int)
        cortes[-1] = self.ciclo_dias
        inicios = np.concatenate([[0], cortes[:-1]])
        return [(int(a), int(b)) for a, b in zip(inicios, cortes, strict=True)]

    def diarios(self) -> dict[str, npt.NDArray[np.float64]]:
        """Parámetros por día del ciclo: Kc, p, trapecio de temperatura y temperatura crítica."""
        n = self.ciclo_dias
        out = {k: np.zeros(n) for k in ("kc", "p", "ta", "tb", "tc", "td")}
        out["t_critica"] = np.full(n, np.nan)
        out["fase"] = np.zeros(n)
        medios, kcs = [], []
        for k, ((a, b), f) in enumerate(zip(self.limites_fases(), self.fases, strict=True)):
            out["ta"][a:b], out["tb"][a:b] = f.temperatura.a, f.temperatura.b
            out["tc"][a:b], out["td"][a:b] = f.temperatura.c, f.temperatura.d
            out["p"][a:b] = f.p if f.p is not None else (self.p or 0.5)
            out["fase"][a:b] = k
            if f.t_critica is not None:
                out["t_critica"][a:b] = f.t_critica
            if f.kc is not None:
                medios.append((a + b - 1) / 2)
                kcs.append(f.kc)
        if self.kc is not None and kcs:
            # Kc por fase en el punto medio de cada una, interpolado linealmente (curva FAO-56).
            out["kc"] = np.interp(np.arange(n), medios, kcs)
        return out


def _f(valor: Any, defecto: float = 0.0) -> float:
    return defecto if valor is None else float(valor)


def _ciclo_dias(doc: dict[str, Any]) -> int | None:
    rango = doc.get("ciclo", {}).get("siembra_a_cosecha_dias")
    if not rango:
        return None
    return round((rango["min"] + rango["max"]) / 2)


def _ky(doc: dict[str, Any], tabla: dict[str, Any]) -> tuple[float, str]:
    fila = tabla["cultivos"].get(doc["_id"])
    if fila:
        return float(fila["ky"]), str(fila["metodo"])
    tol = doc["requerimientos_cultivo"]["suelo"].get("tolerancia_sequia", "media")
    return float(tabla["por_tolerancia_sequia"][tol]), f"convencion_tolerancia_{tol}"


def ficha_de_documento(doc: dict[str, Any], ciclo: int, tabla_ky: dict[str, Any]) -> FichaCultivo:
    req = doc["requerimientos_cultivo"]
    suelo = req["suelo"]
    agua = Trapecio.de(req["agua"])
    agua_dia = agua.escalado(1 / (ciclo if req["agua"]["unidad"] == "mm/ciclo" else 365))
    kc = req.get("kc")
    fases = tuple(
        Fase(
            codigo=f["codigo"],
            fraccion=float(f["fraccion_del_ciclo"]),
            kc=None if f.get("kc") is None else float(f["kc"]),
            p=None if f.get("agotamiento_permitido_p") is None else float(f["agotamiento_permitido_p"]),
            temperatura=Trapecio.de(f["temperatura_aire_C"]),
            t_critica=None if f.get("temp_max_critica_C") is None else float(f["temp_max_critica_C"]),
            lluvia=Trapecio.de(f["precipitacion_mm"]),
            exceso_perjudicial=bool(f.get("exceso_lluvia_perjudicial", False)),
        )
        for f in sorted(doc["fases"], key=lambda f: f["orden"])
    )
    ky, metodo = _ky(doc, tabla_ky)
    n = suelo.get("N_O_CUL")
    alt = doc["zona_produccion"]["altitud_msnm"]
    return FichaCultivo(
        id=doc["_id"], nombre=doc["nombre"], familia=doc["familia"], tipo_ciclo=doc["tipo_ciclo"],
        ciclo_dias=ciclo, fases=fases, temperatura=Trapecio.de(req["temperatura_aire_C"]),
        agua_dia=agua_dia,
        hr=(_f(req["humedad_relativa_pct"]["optimo_min"]), _f(req["humedad_relativa_pct"]["optimo_max"])),
        viento=(_f(req["viento_2m_ms"].get("optimo_max"), 3.0), _f(req["viento_2m_ms"].get("max_tolerable"), 9.0)),
        kc=None if kc is None else (float(kc["ini"]), float(kc["mid"]), float(kc["end"])),
        zr=float(suelo["profundidad_raiz_m"]),
        p=None if suelo.get("agotamiento_permitido_p") is None else float(suelo["agotamiento_permitido_p"]),
        ky=ky, ky_metodo=metodo, ph=Trapecio.de(suelo["ph"]),
        nitrogeno=None if n is None else Trapecio.de(n),
        altitud=(float(alt["min"]), float(alt["max"])),
        t_base=_f(req.get("t_base_C"), 10.0),
        tol_sequia=TOLERANCIA.get(suelo.get("tolerancia_sequia", "media"), 1.0),
        tol_encharcamiento=TOLERANCIA.get(suelo.get("tolerancia_encharcamiento", "media"), 1.0),
        fija_nitrogeno=bool(doc.get("rotacion", {}).get("aporta_nitrogeno", False)),
    )  # fmt: skip


def cargar_documentos(ruta_json: Path) -> tuple[list[dict[str, Any]], str]:
    """Documentos de la colección `cultivos`: de MongoDB si SURCO_MONGO_URI está definida.

    Devuelve también el origen, que queda en el manifiesto del modelo.
    """
    uri = os.environ.get("SURCO_MONGO_URI")
    if uri:
        from pymongo import MongoClient  # extra "mongo"

        base = os.environ.get("SURCO_MONGO_DB", "surco_agro")
        with MongoClient(uri, serverSelectionTimeoutMS=10_000) as cliente:
            docs = list(cliente[base]["cultivos"].find({}))
        log.info("catálogo: %d cultivos desde MongoDB (%s)", len(docs), base)
        return docs, f"mongodb:{base}"
    docs = json.loads(ruta_json.read_text(encoding="utf-8"))
    log.info("catálogo: %d cultivos desde %s", len(docs), ruta_json.name)
    return docs, f"json:{ruta_json.name}"


def seleccionar(
    docs: Sequence[dict[str, Any]], tabla_ky: dict[str, Any], max_ciclo: int, excluir: Sequence[str]
) -> tuple[list[FichaCultivo], dict[str, str]]:
    """Cultivos de ciclo estacional (siembra a cosecha <= max_ciclo días) y por qué quedan fuera los demás."""
    fichas: list[FichaCultivo] = []
    fuera: dict[str, str] = {}
    for doc in sorted(docs, key=lambda d: d["_id"]):
        ciclo = _ciclo_dias(doc)
        if doc["_id"] in excluir:
            fuera[doc["_id"]] = "excluido en pipeline.yaml"
        elif ciclo is None:
            fuera[doc["_id"]] = "perenne: es una decisión de plantación de varios años, no un ciclo estacional"
        elif ciclo > max_ciclo:
            fuera[doc["_id"]] = f"ciclo de {ciclo} días: más largo que el horizonte del modelo ({max_ciclo})"
        elif len(doc["fases"]) > MAX_FASES:
            fuera[doc["_id"]] = f"más de {MAX_FASES} fases"
        else:
            fichas.append(ficha_de_documento(doc, ciclo, tabla_ky))
    return fichas, fuera


def cargar_fichas(ruta_json: Path, ruta_ky: Path, max_ciclo: int, excluir: Sequence[str]
                  ) -> tuple[list[FichaCultivo], dict[str, str], str]:
    docs, origen = cargar_documentos(ruta_json)
    fichas, fuera = seleccionar(docs, cargar_yaml(ruta_ky), max_ciclo, excluir)
    for cid, razon in fuera.items():
        log.info("cultivo fuera del modelo: %s (%s)", cid, razon)
    return fichas, fuera, origen


# --- La ficha como vector de entrada ---------------------------------------------------------

_GLOBALES = (
    "ciclo_dias", "t_a", "t_b", "t_c", "t_d", "agua_a", "agua_b", "agua_c", "agua_d",
    "hr_min", "hr_max", "viento_opt", "viento_tol", "kc_ini", "kc_mid", "kc_end", "tiene_kc",
    "zr", "p", "ky", "ph_a", "ph_b", "ph_c", "ph_d", "n_b", "n_c", "alt_min", "alt_max",
    "t_base", "t_critica_min", "tol_sequia", "tol_encharcamiento", "fija_nitrogeno",
)  # fmt: skip
_POR_FASE = ("fraccion", "t_b", "t_c", "lluvia_b_dia", "lluvia_c_dia", "kc", "p", "activa")
CARACTERISTICAS_CULTIVO = _GLOBALES + tuple(
    f"f{k}_{n}" for k in range(MAX_FASES) for n in _POR_FASE
)


def vector_ficha(f: FichaCultivo) -> F32:
    kc = f.kc or (0.0, 0.0, 0.0)
    criticas = [x.t_critica for x in f.fases if x.t_critica is not None]
    n = f.nitrogeno.tupla() if f.nitrogeno else (0.0, 0.0, 0.0, 0.0)
    globales = [
        f.ciclo_dias, *f.temperatura.tupla(), *f.agua_dia.tupla(), *f.hr, *f.viento, *kc,
        float(f.kc is not None), f.zr, f.p or 0.0, f.ky, *f.ph.tupla(), n[1], n[2], *f.altitud,
        f.t_base, min(criticas) if criticas else 45.0, f.tol_sequia, f.tol_encharcamiento,
        float(f.fija_nitrogeno),
    ]  # fmt: skip
    fases: list[float] = []
    for k in range(MAX_FASES):
        if k < len(f.fases):
            x = f.fases[k]
            dias = max(x.fraccion * f.ciclo_dias, 1.0)
            fases += [x.fraccion, x.temperatura.b, x.temperatura.c, x.lluvia.b / dias,
                      x.lluvia.c / dias, x.kc or 0.0, x.p or 0.0, 1.0]  # fmt: skip
        else:
            fases += [0.0] * len(_POR_FASE)
    return np.array(globales + fases, dtype=np.float32)


def matriz_fichas(fichas: Sequence[FichaCultivo]) -> F32:
    return np.stack([vector_ficha(f) for f in fichas])


def huella(fichas: Sequence[FichaCultivo]) -> str:
    """SHA-256 de los vectores: el bundle sabe con qué fichas se entrenó."""
    return hashlib.sha256(matriz_fichas(fichas).tobytes()).hexdigest()
