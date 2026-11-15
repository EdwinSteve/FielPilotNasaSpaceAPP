"""Etapa `evaluar`: la red frente a B0, Bf, B1 y la red sin pronóstico, en los años de prueba.

- B0: el oráculo sobre la climatología. Es lo que se podría calcular sin ningún modelo.
- Bf: el oráculo sobre cada miembro del pronóstico corregido (el enfoque clásico "pronóstico ->
  modelo de cultivo"). Si la red no le gana, el pronóstico ya dice todo lo que la red sabe.
- B1: la historia del punto para la misma época del año en los años de entrenamiento.
- sin_pronostico: la misma red entrenada sin el conjunto ni Bf (equivale al modelo de FieldPilot).
  La diferencia con la red completa mide CUÁNTO aporta el pronóstico estacional.

Regla de adopción: el backend usa la red solo si le gana a B0 y a Bf en pinball y NDCG@3.
"""

from collections import defaultdict
from dataclasses import dataclass

import numpy as np
import numpy.typing as npt

from surco_planificador.datos_gpu import Muestras
from surco_planificador.etiquetas import DisenoSiembras, Etiquetas
from surco_planificador.metricas import resumen

F32 = npt.NDArray[np.float32]


@dataclass(frozen=True)
class Predicciones:
    rend: F32  # [N, C, 3]
    exito: F32  # [N, C] probabilidad
    riego: F32  # [N, C, 3] mm
    rend_riego: F32  # [N, C, 3]


def de_red(salida: F32, es_probabilidad: bool) -> Predicciones:
    """Salida [N, C, 12] (modelo.SALIDAS_RED) -> predicciones en unidades del agricultor."""
    exito = salida[..., 3] if es_probabilidad else 1 / (1 + np.exp(-salida[..., 3]))
    return Predicciones(salida[..., 0:3], exito, np.expm1(np.maximum(salida[..., 4:7], 0)), salida[..., 9:12])


def de_b0(etq: Etiquetas, m: Muestras) -> Predicciones:
    b = etq.b0[m.p, m.s]
    tres = lambda x: np.repeat(x[..., None], 3, -1)  # noqa: E731 - estimación puntual
    return Predicciones(tres(b[..., 0]), b[..., 1], tres(b[..., 2]), tres(b[..., 5]))


def de_bf(etq: Etiquetas, m: Muestras) -> Predicciones:
    b = etq.bf[m.p, m.s]  # caracteristicas.PRIOR_BF
    riego = np.expm1(b[..., 4])
    return Predicciones(np.stack([b[..., 1], b[..., 0], b[..., 2]], -1), b[..., 3],
                        np.repeat(riego[..., None], 3, -1), np.repeat(b[..., 7:8], 3, -1))  # fmt: skip


def de_b1(etq: Etiquetas, dis: DisenoSiembras, entreno: Muestras, m: Muestras) -> Predicciones:
    semana = np.array([(f.timetuple().tm_yday - 1) // 7 for f in dis.fechas()])
    P, _, C, _ = etq.etiquetas.shape
    tabla = np.full((P, 53, C, 6), np.nan, np.float32)
    grupos: dict[tuple[int, int], list[int]] = defaultdict(list)
    for p, s in zip(entreno.p, entreno.s, strict=True):
        grupos[(int(p), int(semana[s]))].append(int(s))
    for (p, w), ss in grupos.items():
        h = np.where(etq.valido[p, ss][..., None], etq.etiquetas[p, ss], np.nan)
        tabla[p, w, :, 0:3] = np.nanpercentile(h[..., 0], [10, 50, 90], axis=0).T
        tabla[p, w, :, 3] = np.nanmean(h[..., 1], 0)
        tabla[p, w, :, 4] = np.nanmedian(h[..., 2], 0)
        tabla[p, w, :, 5] = np.nanmedian(h[..., 5], 0)
    filas = tabla[m.p, semana[m.s]]
    filas = np.where(np.isnan(filas), np.nanmean(tabla, axis=(0, 1))[None], filas)
    return Predicciones(filas[..., 0:3], filas[..., 3], np.repeat(filas[..., 4:5], 3, -1),
                        np.repeat(filas[..., 5:6], 3, -1))  # fmt: skip


def puntuar(pred: Predicciones, etq: Etiquetas, m: Muestras, filtro: npt.NDArray[np.bool_] | None = None
            ) -> dict[str, float]:
    verdad, mascara = etq.etiquetas[m.p, m.s], etq.valido[m.p, m.s]
    if filtro is not None:
        mascara = mascara & filtro[:, None]
    return resumen(pred.rend, pred.exito, pred.riego, pred.rend_riego, verdad, mascara)


def por_enso(preds: dict[str, Predicciones], etq: Etiquetas, m: Muestras, oni: F32) -> dict[str, object]:
    """Las métricas por fase de ENSO al sembrar: ahí es donde el pronóstico debería ganar."""
    fases = {"nina": oni <= -0.5, "neutral": (oni > -0.5) & (oni < 0.5), "nino": oni >= 0.5}
    return {f: {"casos": int(sel.sum()), **{n: puntuar(p, etq, m, sel) for n, p in preds.items()}}
            for f, sel in fases.items() if sel.sum() > 100}  # fmt: skip


def adoptada(red: dict[str, float], b0: dict[str, float], bf: dict[str, float]) -> bool:
    return all(red["rend_pinball"] < x["rend_pinball"] and red["ndcg_3"] > x["ndcg_3"] for x in (b0, bf))
