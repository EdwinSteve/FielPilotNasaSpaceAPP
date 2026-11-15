"""Vistas tipadas de los YAML de configs/."""

from dataclasses import dataclass
from datetime import date
from pathlib import Path
from typing import Any

import yaml


def cargar_yaml(ruta: Path) -> dict[str, Any]:
    with ruta.open(encoding="utf-8") as f:
        datos = yaml.safe_load(f)
    if not isinstance(datos, dict):
        raise ValueError(f"{ruta} debe contener un mapa")
    return datos


@dataclass(frozen=True)
class Anios:
    primero: int
    ultimo: int

    def contiene(self, anio: int) -> bool:
        return self.primero <= anio <= self.ultimo


@dataclass(frozen=True)
class ConfigPronostico:
    dataset: str
    centro: str
    sistema: str
    primer_anio: int
    ultimo_anio: int
    miembros: int
    dias_lead: int
    area: tuple[float, float, float, float]
    dia_disponible: int
    descargas_paralelas: int
    bloque_lead_dias: int

    def inicios(self) -> list[tuple[int, int]]:
        """(año, mes) de cada inicio de pronóstico, en orden."""
        return [(a, m) for a in range(self.primer_anio, self.ultimo_anio + 1) for m in range(1, 13)]


@dataclass(frozen=True)
class ConfigPipeline:
    max_puntos: int
    semilla: int
    primer_dia: date
    ultimo_dia: date
    gradiente_c_por_km: float
    max_hueco_dias: int
    max_fraccion_enmascarada: float
    radio_suelo_km: float
    pronostico: ConfigPronostico
    desplazamientos: tuple[int, ...]
    dias_previos: int
    umbral_exito: float
    max_ciclo_dias: int
    excluir: tuple[str, ...]
    procesos: int
    anios_base: Anios
    ventana_suavizado: int
    entrenamiento: Anios
    validacion: Anios
    prueba: Anios
    departamentos_reservados: tuple[str, ...]


def cargar_pipeline(ruta: Path) -> ConfigPipeline:
    r = cargar_yaml(ruta)
    p, c, e, d = r["pronostico"], r["clima"], r["etiquetas"], r["divisiones"]
    return ConfigPipeline(
        max_puntos=int(r["puntos"]["max_puntos"]),
        semilla=int(r["puntos"]["semilla"]),
        primer_dia=c["primer_dia"],
        ultimo_dia=c["ultimo_dia"],
        gradiente_c_por_km=float(c["gradiente_c_por_km"]),
        max_hueco_dias=int(c["max_hueco_dias"]),
        max_fraccion_enmascarada=float(c["max_fraccion_enmascarada"]),
        radio_suelo_km=float(r["suelo"]["radio_busqueda_km"]),
        pronostico=ConfigPronostico(
            dataset=str(p["dataset"]), centro=str(p["centro"]), sistema=str(p["sistema"]),
            primer_anio=int(p["primer_anio"]), ultimo_anio=int(p["ultimo_anio"]),
            miembros=int(p["miembros"]), dias_lead=int(p["dias_lead"]),
            area=tuple(float(x) for x in p["area"]),  # type: ignore[arg-type]
            dia_disponible=int(p["dia_disponible"]),
            descargas_paralelas=int(p["descargas_paralelas"]),
            bloque_lead_dias=int(p["bloque_lead_dias"]),
        ),
        desplazamientos=tuple(int(x) for x in e["desplazamientos_siembra"]),
        dias_previos=int(e["dias_previos_balance"]),
        umbral_exito=float(e["umbral_exito"]),
        max_ciclo_dias=int(e["max_ciclo_dias"]),
        excluir=tuple(str(x) for x in e.get("excluir", [])),
        procesos=int(e.get("procesos", 4)),
        anios_base=Anios(*r["climatologia"]["anios_base"]),
        ventana_suavizado=int(r["climatologia"]["ventana_suavizado"]),
        entrenamiento=Anios(*d["entrenamiento"]),
        validacion=Anios(*d["validacion"]),
        prueba=Anios(*d["prueba"]),
        departamentos_reservados=tuple(str(x) for x in d["departamentos_reservados"]),
    )
