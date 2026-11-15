"""Métricas de evaluación en NumPy puro (se prueban sin GPU). Todas aceptan una máscara [N, C]."""

import numpy as np
import numpy.typing as npt

CUANTILES = np.array([0.1, 0.5, 0.9])
A = npt.NDArray[np.floating]
M = npt.NDArray[np.bool_]


def pinball(pred: A, obj: A, m: M) -> float:
    d = obj[..., None] - pred
    return float(np.maximum(CUANTILES * d, (CUANTILES - 1) * d).mean(-1)[m].mean())


def brier(prob: A, y: A, m: M) -> float:
    return float(((prob - y) ** 2)[m].mean())


def ece(prob: A, y: A, m: M, bins: int = 10) -> float:
    prob, y = prob[m], y[m]
    cual = np.clip(np.digitize(prob, np.linspace(0, 1, bins + 1)[1:-1]), 0, bins - 1)
    return float(sum((cual == b).mean() * abs(prob[cual == b].mean() - y[cual == b].mean())
                     for b in range(bins) if (cual == b).any()))  # fmt: skip


def cobertura(bajo: A, alto: A, obj: A, m: M) -> float:
    return float(((obj >= bajo) & (obj <= alto))[m].mean())


def ndcg(puntajes: A, relevancia: A, m: M, k: int = 3) -> float:
    """NDCG@k por fecha-sitio sobre los cultivos válidos: ¿el ranking pone arriba a los mejores?"""
    p = np.where(m, puntajes, -np.inf)
    r = np.where(m, relevancia, 0.0)
    desc = 1 / np.log2(np.arange(2, k + 2))
    orden = np.argsort(-p, axis=1)[:, :k]
    dcg = (np.take_along_axis(r, orden, 1) * desc).sum(1)
    idcg = (np.sort(r, 1)[:, ::-1][:, :k] * desc).sum(1)
    return float(np.where(idcg > 0, dcg / np.where(idcg > 0, idcg, 1), 1.0).mean())


def acierto_top1(puntajes: A, relevancia: A, m: M, tolerancia: float = 0.05) -> float:
    """Fracción de casos en que el cultivo elegido queda a menos de `tolerancia` del mejor real."""
    elegido = np.argmax(np.where(m, puntajes, -np.inf), axis=1)
    mejor = np.where(m, relevancia, -np.inf).max(1)
    return float((relevancia[np.arange(len(elegido)), elegido] >= mejor - tolerancia).mean())


def resumen(rend: A, exito: A, riego: A, rend_riego: A, verdad: A, m: M) -> dict[str, float]:
    """rend, riego, rend_riego [N, C, 3] (riego en mm); exito [N, C]; verdad [N, C, 6] (etiquetas)."""
    return {
        "rend_pinball": round(pinball(rend, verdad[..., 0], m), 5),
        "rend_mae": round(float(np.abs(rend[..., 1] - verdad[..., 0])[m].mean()), 5),
        "rend_cobertura_p10_p90": round(cobertura(rend[..., 0], rend[..., 2], verdad[..., 0], m), 4),
        "exito_brier": round(brier(exito, verdad[..., 1], m), 5),
        "exito_ece": round(ece(exito, verdad[..., 1], m), 5),
        "riego_mae_mm": round(float(np.abs(riego[..., 1] - verdad[..., 2])[m].mean()), 2),
        "rend_riego_pinball": round(pinball(rend_riego, verdad[..., 5], m), 5),
        "ndcg_3": round(ndcg(rend[..., 1], verdad[..., 0], m), 5),
        "acierto_top1": round(acierto_top1(rend[..., 1], verdad[..., 0], m), 4),
    }
