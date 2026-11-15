"""Piezas del servicio sin red: el mapeo de Bf, el nitrógeno del catálogo y las fallas de las fuentes."""

import urllib.error

import numpy as np
import pytest

from surco_planificador.caracteristicas import PRIOR, PRIOR_B0
from surco_planificador.errores import FuenteCaida
from surco_planificador.inferencia import desde_bf
from surco_planificador.servicio import decimal, desde_fuente, kg_fijacion


class Ficha:
    def __init__(self, id_: str) -> None:
        self.id = id_


def test_bf_usa_las_columnas_bf_del_prior_como_evaluar_de_bf() -> None:
    prior = np.zeros((2, len(PRIOR)), np.float32)
    bf = len(PRIOR_B0)
    # bf_rend_media, bf_rend_p10, bf_rend_p90, bf_exito, bf_log1p_riego, calor, frío, bf_rend_riego
    prior[0, bf:] = [0.7, 0.5, 0.9, 0.8, np.log1p(120.0), 0.0, 0.0, 0.95]
    recs = desde_bf([Ficha("maiz"), Ficha("frijol")], prior)  # type: ignore[list-item]
    r = recs[0]
    assert (r.cultivo, r.rend_p10, r.rend_p50, r.rend_p90) == ("maiz", 0.5, pytest.approx(0.7), pytest.approx(0.9))
    assert r.prob_exito == pytest.approx(0.8) and r.riego_mm_p50 == pytest.approx(120.0, rel=1e-4)
    assert r.rend_con_riego_p50 == pytest.approx(0.95)
    assert recs[1].cultivo == "frijol" and recs[1].prob_exito == 0.0


def test_fijacion_de_nitrogeno_del_catalogo() -> None:
    docs = [{"_id": "frijol", "nitrogeno": {"fijacion_biologica_kg_ha": {"min": 40, "max": 80}}},
            {"_id": "maiz", "nitrogeno": {"fijacion_biologica_kg_ha": None}}, {"_id": "yuca"}]
    assert kg_fijacion(docs) == {"frijol": 60, "maiz": None, "yuca": None}


def test_las_fallas_de_red_dicen_que_fuente_cayo() -> None:
    def caida() -> None:
        raise urllib.error.URLError("timed out")

    with pytest.raises(FuenteCaida) as e:
        desde_fuente("SoilGrids", caida)
    assert e.value.fuente == "SoilGrids" and "SoilGrids no respondió" in str(e.value)
    assert desde_fuente("NOAA (ONI)", lambda: 3) == 3


def test_decimal_con_coma() -> None:
    assert decimal(5.5) == "5,5" and decimal(7) == "7,0"
