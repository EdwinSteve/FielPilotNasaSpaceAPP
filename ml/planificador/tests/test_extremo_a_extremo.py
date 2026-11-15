"""La tubería completa con datos sintéticos, en CPU: etiquetas -> entrenar -> ablación -> evaluar ->
exportar ONNX -> recomendar con el bundle. Necesita torch y onnxruntime (se salta sin ellos)."""

import json
import shutil
from datetime import date, timedelta
from pathlib import Path

import numpy as np
import pytest

from surco_planificador.caracteristicas import CANALES_HISTORIA, SITIO
from surco_planificador.config import Anios
from surco_planificador.datos import DatosLimpios
from surco_planificador.fuentes.c3s import CANALES_CRUDOS
from surco_planificador.limpieza.climatologia import servida, sin_fuga
from surco_planificador.rutas import Rutas

RAIZ = Rutas.por_defecto()
CULTIVOS = ("maiz", "frijol", "papa", "name", "lechuga")
PIPELINE = """
puntos: {max_puntos: 10, semilla: 7}
clima: {primer_dia: 1981-01-01, ultimo_dia: 1987-12-31, gradiente_c_por_km: -6.5, max_hueco_dias: 3,
        max_fraccion_enmascarada: 0.02}
suelo: {radio_busqueda_km: 10}
pronostico: {dataset: x, centro: ecmwf, sistema: "51", primer_anio: 1982, ultimo_anio: 1987, miembros: 4,
             dias_lead: 215, area: [14, -80, -5, -66], dia_disponible: 7, descargas_paralelas: 1,
             bloque_lead_dias: 30}
etiquetas: {desplazamientos_siembra: [6, 11, 16, 21, 26, 31], dias_previos_balance: 30, umbral_exito: 0.6,
            max_ciclo_dias: 365, excluir: [], procesos: 1}
climatologia: {anios_base: [1981, 1987], ventana_suavizado: 31}
divisiones: {entrenamiento: [1982, 1984], validacion: [1985, 1985], prueba: [1986, 1987],
             departamentos_reservados: ["99"]}
"""
ENTRENAMIENTO = """
modelo: {canales_historia: 16, dilataciones: [1, 2], canales_pronostico: 16, dim_miembro: 16,
         dim_conjunto: 16, canales_climatologia: 8, oculto_estatico: 16, embebido: 32, oculto_cabeza: 32,
         dropout: 0.0}
optimizacion: {tamano_lote: 64, lr: 0.003, decaimiento_pesos: 0.0, fraccion_calentamiento: 0.1,
               recorte_gradiente: 1.0, max_epocas: 2, paciencia: 2, miembros_min: 2}
pesos_perdida: {rendimiento: 1.0, rendimiento_riego: 0.5, exito: 0.5, riego: 0.5, estres: 0.2}
ejecucion: {precision: bf16, compilar: "off", semilla: 7, deterministico_final: false,
            semillas_conjunto: [7, 11], pronostico_en: auto}
ajuste: {pruebas: 1, epocas: 1, espacio: {lr: [0.001, 0.003], canales_historia: [16],
         canales_pronostico: [16], dropout: [0.0, 0.1], tamano_lote: [64]}}
"""


def _clima_sintetico(rng: np.random.Generator, dias: int, dia0: date, t_media: float) -> np.ndarray:
    doy = np.array([(dia0 + timedelta(days=i)).timetuple().tm_yday for i in range(dias)])
    t = t_media + 1.5 * np.sin(2 * np.pi * doy / 365) + rng.normal(0, 1, dias)
    lluvia_media = 4 + 3 * np.cos(4 * np.pi * doy / 365)  # régimen bimodal andino
    c = {"t2m": t, "tmax": t + 5, "tmin": t - 5, "tdew": t - 4, "lluvia": rng.gamma(0.7, lluvia_media / 0.7),
         "hr": np.full(dias, 75.0), "viento": np.full(dias, 2.0), "radiacion": np.full(dias, 18.0),
         "et0": 3.5 + 0.1 * (t - t_media), "gwetroot": np.full(dias, 0.5), "radiacion_estimada": np.zeros(dias)}
    return np.stack([c[k] for k in CANALES_HISTORIA], -1).astype(np.float32)


def _preparar(tmp: Path) -> Rutas:
    configs = tmp / "configs"
    configs.mkdir()
    (configs / "pipeline.yaml").write_text(PIPELINE, encoding="utf-8")
    (configs / "entrenamiento.yaml").write_text(ENTRENAMIENTO, encoding="utf-8")
    shutil.copy(RAIZ.configs / "ky_fao33.yaml", configs)
    docs = [d for d in json.loads(RAIZ.catalogo_json.read_text(encoding="utf-8")) if d["_id"] in CULTIVOS]
    (tmp / "cultivos.json").write_text(json.dumps(docs), encoding="utf-8")
    rutas = Rutas(configs, tmp / "datos", tmp / "cultivos.json", tmp / "divipola.json", tmp / "modelos")
    rng = np.random.default_rng(0)
    dia0, fin = date(1981, 1, 1), date(1987, 12, 31)
    D, P = (fin - dia0).days + 1, 4
    clima = np.stack([_clima_sintetico(rng, D, dia0, t) for t in (26, 21, 16, 12)])
    h = {c: i for i, c in enumerate(CANALES_HISTORIA)}
    anios = range(1982, 1988)
    clim = np.stack([sin_fuga({k: clima[p, :, h[c]].astype(np.float64) for k, c in
                               (("t", "t2m"), ("lluvia", "lluvia"), ("tmax", "tmax"), ("tmin", "tmin"), ("et0", "et0"))},
                              dia0, Anios(1981, 1987), anios, 31) for p in range(P)])  # fmt: skip
    perfil = np.tile(np.array([40, 30, 15, 6.0, 1.3, 35, 20, 1.5, 15], np.float32), (P, 6, 1))
    meses = [(a, m) for a in range(1950, 1990) for m in range(1, 13)]
    DatosLimpios(
        ids=np.array([f"p{i:03d}" for i in range(P)]), departamentos=np.array(["41", "41", "05", "05"]),
        lat=np.array([3.0, 4.0, 5.0, 6.0], np.float32), lon=np.full(P, -75.0, np.float32),
        elevacion=np.array([500, 1300, 2200, 2900], np.float32), dia0=dia0, clima=clima,
        valido=np.ones((P, D), bool), sitio=rng.normal(0, 1, (P, len(SITIO))).astype(np.float32),
        perfil_suelo=perfil, oni_meses=np.array([a * 12 + m - 1 for a, m in meses], np.int32),
        oni_valores=np.sin(np.arange(len(meses)) / 9).astype(np.float32), anios_clim=np.array(list(anios), np.int32),
        clim=clim,
    ).guardar(rutas.limpio)  # fmt: skip
    # Pronóstico sintético: la verdad con sesgo y ruido por miembro (para que sea "hábil" pero imperfecto).
    inicios = [date(a, m, 1) for a in anios for m in range(1, 13)]
    c = {k: CANALES_HISTORIA.index(k) for k in ("tmax", "tmin", "tdew", "lluvia", "radiacion", "viento")}
    crudo = np.zeros((len(inicios), P, 4, 215, len(CANALES_CRUDOS)), np.float16)
    for i, ini in enumerate(inicios):
        a = (ini - dia0).days
        obs = clima[:, a : a + 215][..., [c[k] for k in c]]
        dentro = obs.shape[1]
        ruido = rng.normal(0, 1.5, (P, 4, dentro, len(CANALES_CRUDOS)))
        x = obs[:, None] + 1.0 + ruido
        x[..., 3] = np.maximum(obs[:, None, :, 3] * 0.7 + rng.gamma(1, 1, (P, 4, dentro)), 0)
        crudo[i, :, :, :dentro] = x
        crudo[i, :, :, dentro:] = x[:, :, -1:]  # el último año se sale de POWER: se rellena (queda sin etiquetas)
    rutas.pronosticos.mkdir(parents=True)
    np.save(rutas.pronosticos / "crudo.npy", crudo)
    np.save(rutas.pronosticos / "hechos.npy", np.ones(len(inicios), bool))
    return rutas


def test_tuberia_completa(tmp_path: Path) -> None:
    pytest.importorskip("torch")
    pytest.importorskip("onnxruntime")
    from surco_planificador.inferencia import Lote, Planificador
    from surco_planificador.tuberia import Tuberia

    rutas = _preparar(tmp_path)
    tub = Tuberia(rutas, "prueba")
    assert [f.id for f in tub.fichas] == sorted(CULTIVOS)
    tub.correr(("etiquetas", "entrenar", "ablacion", "evaluar", "exportar"))

    metricas = json.loads((rutas.corridas / "prueba" / "metricas.json").read_text(encoding="utf-8"))
    assert {"red", "b0_climatologia", "bf_pronostico", "b1_historia", "red_sin_pronostico"} <= set(metricas["prueba"])
    for fila in metricas["prueba"].values():
        assert all(np.isfinite(v) for v in fila.values())

    bundle = next(rutas.modelos.iterdir())
    manifiesto = json.loads((bundle / "manifiesto.json").read_text(encoding="utf-8"))
    assert manifiesto["paridad_onnx"] < 1e-4

    # Servicio en CPU con el bundle: un lote del punto 2 (2200 m), con 9 miembros (más que en el entrenamiento).
    datos = DatosLimpios.cargar(rutas.limpio)
    plan = Planificador(bundle, tub.fichas)
    inicio, siembra = date(1986, 3, 1), date(1986, 3, 12)
    i_hist = datos.indice(siembra)
    h = {k: CANALES_HISTORIA.index(k) for k in ("tmax", "tmin", "tdew", "lluvia", "radiacion", "viento")}
    a = datos.indice(inicio)
    crudo = np.repeat(datos.clima[2, a : a + 215][None][..., list(h.values())], 9, 0) + 0.5
    lote = Lote(lat=5.0, lon=-75.0, elevacion_m=2200, sitio=datos.sitio[2], historia=datos.clima[2, i_hist - 365 : i_hist],
                clim=servida({k: datos.clima[2, :, CANALES_HISTORIA.index(c)].astype(np.float64) for k, c in
                              (("t", "t2m"), ("lluvia", "lluvia"), ("tmax", "tmax"), ("tmin", "tmin"), ("et0", "et0"))},
                             datos.dia0, Anios(1981, 1987), 31),
                ph_suelo=6.0, taw_por_cultivo={f.id: 100.0 for f in tub.fichas})  # fmt: skip
    diario = plan.corregir(crudo.astype(np.float32), inicio, lote)
    recs = plan.recomendar(lote, diario, inicio, siembra, oni=0.3, oni_tendencia=0.1)
    assert len(recs) == len(CULTIVOS)
    assert recs[0].rend_p50 >= recs[-1].rend_p50
    assert all(0 <= r.prob_exito <= 1 and r.rend_p10 <= r.rend_p50 <= r.rend_p90 for r in recs)
