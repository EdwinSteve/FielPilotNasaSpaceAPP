"""Fugas: nada que el agricultor no sabría el día de siembra entra a la red."""

from datetime import date, timedelta

import numpy as np

from surco_planificador.caracteristicas import oni_conocido
from surco_planificador.config import Anios, cargar_pipeline
from surco_planificador.etiquetas import diseno
from surco_planificador.limpieza.climatologia import sin_fuga
from surco_planificador.rutas import Rutas

CFG = cargar_pipeline(Rutas.por_defecto().configs / "pipeline.yaml")


def test_siembras_usan_un_pronostico_ya_publicado_y_el_mas_reciente() -> None:
    dis = diseno(CFG)
    for inicio, o in zip(np.repeat(dis.inicios, len(dis.desplazamientos)),
                         np.tile(dis.desplazamientos, len(dis.inicios)), strict=True):  # fmt: skip
        siembra = inicio + timedelta(days=int(o))
        publicado = inicio.replace(day=CFG.pronostico.dia_disponible)
        assert siembra >= publicado, "se usaría un pronóstico que aún no existía"
        siguiente = (inicio.replace(day=28) + timedelta(days=4)).replace(day=CFG.pronostico.dia_disponible)
        assert siembra < siguiente, "había un pronóstico más nuevo: el agricultor usaría ese"


def test_climatologia_excluye_el_anio_de_siembra_y_el_siguiente() -> None:
    dia0 = date(2000, 1, 1)
    dias = (date(2006, 1, 1) - dia0).days
    t = np.zeros(dias)
    for a in range(2000, 2006):  # cada año con su propia temperatura constante
        ini, fin = (date(a, 1, 1) - dia0).days, (date(a + 1, 1, 1) - dia0).days
        t[ini:fin] = a - 2000
    series = {k: t.copy() for k in ("t", "lluvia", "tmax", "tmin", "et0")}
    clim = sin_fuga(series, dia0, Anios(2000, 2005), range(2002, 2003), ventana=1)
    # años base sin 2002 ni 2003: 0, 1, 4, 5 -> media 2.5
    assert abs(float(clim[0, 100, 0]) - 2.5) < 1e-5


def test_oni_es_de_un_trimestre_ya_publicado() -> None:
    oni = {(2020, m): float(m) for m in range(1, 13)}
    ultimo, _ = oni_conocido({**oni, **{(2019, m): 0.0 for m in range(1, 13)}}, date(2020, 6, 10))
    assert ultimo == 4.0  # trimestre que terminó en abril, publicado a comienzos de mayo


def test_division_temporal_sin_solapes() -> None:
    assert CFG.entrenamiento.ultimo < CFG.validacion.primero <= CFG.validacion.ultimo < CFG.prueba.primero
    # la prueba solo ve pronósticos operativos (SEAS5 en tiempo real desde 2017)
    assert CFG.prueba.primero >= 2017
