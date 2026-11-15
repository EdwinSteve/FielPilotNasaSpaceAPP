"""Fichas desde el catálogo surco_agro (JSON versionado; MongoDB si SURCO_MONGO_URI está definida)."""

import numpy as np

from surco_planificador.catalogo import CARACTERISTICAS_CULTIVO, cargar_fichas, matriz_fichas
from surco_planificador.rutas import Rutas

R = Rutas.por_defecto()


def _fichas():  # noqa: ANN202
    return cargar_fichas(R.catalogo_json, R.configs / "ky_fao33.yaml", 365, ["arroz_riego"])


def test_solo_ciclos_estacionales() -> None:
    fichas, fuera, _ = _fichas()
    ids = {f.id for f in fichas}
    assert {"maiz", "frijol", "papa", "tomate", "algodon", "yuca"} <= ids
    assert "cafe" in fuera and "palma_aceite" in fuera and "arroz_riego" in fuera
    assert all(f.ciclo_dias <= 365 for f in fichas)


def test_fases_suman_el_ciclo() -> None:
    for f in _fichas()[0]:
        limites = f.limites_fases()
        assert limites[0][0] == 0 and limites[-1][1] == f.ciclo_dias
        assert abs(sum(x.fraccion for x in f.fases) - 1) < 1e-6
        assert all(a < b for a, b in limites)


def test_vector_finito_y_con_nombres() -> None:
    m = matriz_fichas(_fichas()[0])
    assert m.shape[1] == len(CARACTERISTICAS_CULTIVO)
    assert np.isfinite(m).all()


def test_kc_diario_sigue_las_fases() -> None:
    maiz = next(f for f in _fichas()[0] if f.id == "maiz")
    kc = maiz.diarios()["kc"]
    assert kc.min() >= 0.3 - 1e-6 and kc.max() <= 1.2 + 1e-6
    assert kc[len(kc) // 2] > kc[0]  # sube del establecimiento a la floración
