"""Corrección de sesgo, unidades de C3S y péntadas."""

from datetime import date

import numpy as np

from surco_planificador.fuentes.c3s import interpolar, pesos_bilineales
from surco_planificador.pronostico import (
    LLUVIA,
    ajustar_correccion,
    aplicar_correccion,
    mapear_cuantiles,
)


def test_bilineal_en_grilla_norte_a_sur() -> None:
    lats = np.array([5.0, 4.0, 3.0], np.float32)  # como vienen del CDS
    lons = np.array([-76.0, -75.0], np.float32)
    campo = np.zeros((1, 1, 3, 2, 1), np.float32)
    campo[0, 0, :, :, 0] = lats[:, None] * 10 + lons[None, :]
    v = interpolar(campo, lats, lons, [(4.5, -75.5)])[0, 0, 0, 0]
    assert abs(v - (45 - 75.5)) < 1e-4
    celdas, pesos = pesos_bilineales(lats, lons, 4.0, -76.0)
    assert abs(sum(pesos) - 1) < 1e-6


def test_mapeo_de_cuantiles_monotono_y_con_ceros() -> None:
    qf = np.quantile(np.r_[np.zeros(50), np.linspace(0.1, 20, 50)], np.linspace(0, 1, 101)).astype(np.float32)
    qo = np.quantile(np.r_[np.zeros(70), np.linspace(1, 40, 30)], np.linspace(0, 1, 101)).astype(np.float32)
    x = np.linspace(0, 30, 200).astype(np.float32)
    y = mapear_cuantiles(x, qf, qo)
    assert (np.diff(y) >= -1e-4).all() and y[0] == 0 and (y >= 0).all()


def test_correccion_recupera_media_y_desviacion() -> None:
    rng = np.random.default_rng(1)
    I, M, T, C = 48, 5, 60, 6  # noqa: E741 - inicios, miembros, días, canales
    verdad = rng.normal(25, 2, (I, T, C)).astype(np.float32)
    verdad[..., LLUVIA] = rng.gamma(0.8, 6, (I, T))
    fc = (verdad[:, None] * 0.5 + 10 + rng.normal(0, 1, (I, M, T, C))).astype(np.float32)  # sesgado
    fc[..., LLUVIA] = rng.gamma(2.0, 1.0, (I, M, T))  # llovizna permanente, como los modelos
    meses = np.tile(np.arange(1, 13), 4)
    usar = np.arange(I) < 36
    cor = ajustar_correccion(fc, verdad, meses, usar, 30)
    corregido = aplicar_correccion(fc, meses, cor)
    t = corregido[..., 0]
    assert abs(t.mean() - verdad[..., 0].mean()) < 0.3
    assert abs(t.std() - verdad[..., 0].std()) < 0.3
    seco_obs = (verdad[..., LLUVIA] < 0.1).mean()
    seco_cor = (corregido[..., LLUVIA] < 0.1).mean()
    assert abs(seco_cor - seco_obs) < 0.08  # la llovizna se vuelve días secos
    assert (corregido[..., 1] <= corregido[..., 0] - 0.1 + 1e-4).all()  # tmin < tmax


def test_inicios_ordenados() -> None:
    from surco_planificador.config import cargar_pipeline
    from surco_planificador.rutas import Rutas

    cfg = cargar_pipeline(Rutas.por_defecto().configs / "pipeline.yaml")
    inicios = cfg.pronostico.inicios()
    meses = cfg.pronostico.meses_inicio
    assert inicios[0] == (cfg.pronostico.primer_anio, meses[0]) and len(inicios) % len(meses) == 0
    assert all(date(*b, 1) > date(*a, 1) for a, b in zip(inicios, inicios[1:]))
