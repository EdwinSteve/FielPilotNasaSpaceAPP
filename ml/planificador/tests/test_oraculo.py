"""El oráculo: balance hídrico FAO-56 y factores por fase con fichas reales del catálogo."""

from pathlib import Path

import numpy as np
import pytest

from surco_planificador.catalogo import cargar_fichas
from surco_planificador.oraculo import CicloClima, Oraculo, balance_hidrico, trapecio
from surco_planificador.rutas import Rutas

RUTAS = Rutas.por_defecto()


@pytest.fixture(scope="module")
def fichas() -> dict:
    f, _, _ = cargar_fichas(RUTAS.catalogo_json, RUTAS.configs / "ky_fao33.yaml", 365, ["arroz_riego"])
    return {x.id: x for x in f}


def clima(n: int, dias: int, t: float, lluvia: float, et0: float = 4.0, tmin: float | None = None) -> CicloClima:
    def lleno(v: float) -> np.ndarray:
        return np.full((n, dias), v, np.float32)

    return CicloClima(lleno(t), lleno(t + 6), lleno(t - 6 if tmin is None else tmin), lleno(lluvia), lleno(et0),
                      np.full((n, 30), lluvia, np.float32), np.full((n, 30), et0, np.float32))


def test_trapecio() -> None:
    x = np.array([5, 10, 20, 30, 35, 40], np.float32)
    np.testing.assert_allclose(trapecio(x, 10, 18, 30, 40), [0, 0, 1, 1, 0.5, 0], atol=1e-6)


def test_balance_humedo_sin_estres_seco_con_estres() -> None:
    etc = np.full((1, 60), 5.0, np.float32)
    p = np.full(60, 0.5)
    eta_h, etc_h, riego_h, _ = balance_hidrico(np.full((1, 60), 10.0, np.float32), etc, 100, p, np.zeros(1, np.float32), False)
    assert eta_h[0] == pytest.approx(etc_h[0])
    eta_s, etc_s, _, _ = balance_hidrico(np.zeros((1, 60), np.float32), etc, 100, p, np.zeros(1, np.float32), False)
    assert eta_s[0] < 0.6 * etc_s[0]
    _, _, riego, _ = balance_hidrico(np.zeros((1, 60), np.float32), etc, 100, p, np.zeros(1, np.float32), True)
    assert riego[0] > 200  # regar repone casi toda la ETc


def test_maiz_bien_en_clima_calido_y_humedo(fichas: dict) -> None:
    r = Oraculo(fichas["maiz"], 0.6).simular(clima(1, 140, 25, 5.5), taw=120, ph_suelo=6.5)
    assert r[0, 0] > 0.8 and r[0, 1] == 1


def test_papa_peor_en_tierra_caliente(fichas: dict) -> None:
    o = Oraculo(fichas["papa"], 0.6)
    frio = o.simular(clima(1, 150, 15, 4.5), taw=80, ph_suelo=5.5)[0, 0]
    caliente = o.simular(clima(1, 150, 28, 4.5), taw=80, ph_suelo=5.5)[0, 0]
    assert frio > 0.6 > caliente


def test_sequia_baja_el_secano_pero_no_el_riego(fichas: dict) -> None:
    r = Oraculo(fichas["maiz"], 0.6).simular(clima(1, 140, 25, 0.3, et0=5), taw=120, ph_suelo=6.5)[0]
    assert r[0] < 0.4 and r[5] > 0.8 and r[2] > 300


def test_helada_y_exceso_de_lluvia_penalizan(fichas: dict) -> None:
    o = Oraculo(fichas["papa"], 0.6)
    normal = o.simular(clima(1, 150, 14, 4.5, tmin=6), 80, 5.5)[0, 5]
    helada = o.simular(clima(1, 150, 14, 4.5, tmin=-1), 80, 5.5)[0, 5]
    diluvio = o.simular(clima(1, 150, 14, 30), 80, 5.5)[0, 5]
    assert helada < 0.1 < normal and diluvio < normal


def test_cultivo_sin_kc_usa_trapecios_de_lluvia(fichas: dict) -> None:
    assert not fichas["name"].balance_hidrico
    o = Oraculo(fichas["name"], 0.6)
    humedo = o.simular(clima(1, 270, 27, 5), 60, 6.0)[0, 0]
    seco = o.simular(clima(1, 270, 27, 0.2), 60, 6.0)[0, 0]
    assert humedo > seco


def test_vectorizado_igual_a_uno_por_uno(fichas: dict) -> None:
    o = Oraculo(fichas["frijol"], 0.6)
    rng = np.random.default_rng(0)
    c = clima(5, 115, 20, 3)
    c = CicloClima(c.t + rng.normal(0, 2, c.t.shape).astype(np.float32), c.tmax, c.tmin,
                   rng.gamma(1, 3, c.lluvia.shape).astype(np.float32), c.et0, c.lluvia_previa, c.et0_previa)
    junto = o.simular(c, 90, 6.0)
    for i in range(5):
        solo = CicloClima(*(getattr(c, k)[i : i + 1] for k in ("t", "tmax", "tmin", "lluvia", "et0",
                                                                "lluvia_previa", "et0_previa")))
        np.testing.assert_allclose(junto[i], o.simular(solo, 90, 6.0)[0], rtol=1e-5)


def test_catalogo_en_repo() -> None:
    assert Path(RUTAS.catalogo_json).exists()
