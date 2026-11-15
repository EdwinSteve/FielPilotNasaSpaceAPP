"""Servicio por coordenadas: el pronóstico de análogos se arma igual que en el entrenamiento y sin fugas."""

from datetime import date

import numpy as np

from surco_planificador.servicio import elegir_analogos, inicio_para, oni_al

DIA0 = date(1981, 1, 1)
N_DIAS = (date(2026, 1, 1) - DIA0).days


def _oni() -> dict[tuple[int, int], float]:
    # cada año con un ONI constante distinto: 1997 y 2015 los más cálidos
    return {(a, m): (2.0 if a in (1997, 2015) else (a % 7 - 3) / 3) for a in range(1980, 2027) for m in range(1, 13)}


def test_inicio_es_el_pronostico_ya_publicado() -> None:
    assert inicio_para(date(2026, 3, 12), 7) == date(2026, 3, 1)
    assert inicio_para(date(2026, 3, 7), 7) == date(2026, 3, 1)
    assert inicio_para(date(2026, 3, 3), 7) == date(2026, 2, 1)  # el de marzo aún no salía
    assert inicio_para(date(2026, 1, 2), 7) == date(2025, 12, 1)


def test_analogos_excluyen_el_anio_y_sus_vecinos_y_prefieren_oni_parecido() -> None:
    oni = _oni()
    arranques = elegir_analogos(oni, date(2016, 1, 1), DIA0, N_DIAS, 10, 215)
    anios = [(DIA0.toordinal() + s) for s in arranques]
    anios = [date.fromordinal(a).year for a in anios]
    assert len(set(anios)) == 10
    assert not {2015, 2016, 2017} & set(anios)
    # en enero se conoce el ONI de fin del año anterior: ene-2016 ve El Niño 2015 y su mejor
    # análogo es ene-1998, que veía El Niño 1997
    assert anios[0] == 1998
    assert all(date.fromordinal(DIA0.toordinal() + s).month == 1 for s in arranques)


def test_analogos_caben_en_la_serie() -> None:
    arranques = elegir_analogos(_oni(), date(2025, 9, 1), DIA0, N_DIAS, 25, 215)
    assert max(arranques) + 215 <= N_DIAS
    assert min(arranques) >= 0


def test_oni_de_fecha_futura_usa_el_ultimo_publicado() -> None:
    oni = {(2026, m): float(m) for m in range(1, 10)} | {(2025, m): 0.0 for m in range(1, 13)}
    valor, _ = oni_al(oni, date(2027, 3, 1))
    assert valor == 9.0
    assert np.isfinite(valor)
