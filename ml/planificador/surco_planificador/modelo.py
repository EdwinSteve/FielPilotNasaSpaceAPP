"""PlanificadorEstacionalNet: historia del lote + conjunto de pronóstico + climatología + suelo,
cruzados con la ficha de cada cultivo.

            historia 365 d ──► TCN dilatada ──► pool atencional ─┐
  pronóstico M miembros × 35 péntadas ──► CNN por miembro ──►    │
          (DeepSets: media, desviación y atención sobre miembros) ┼─► sitio-fecha (256)
          climatología 73 péntadas ──► CNN ──► media ────────────┤          │
          suelo, sitio, ENSO, época ──► MLP ─────────────────────┘          │ FiLM
                                         ficha del cultivo ──► MLP ──► γ, β ┘
                         [cruzado, prior B0 + Bf] ──► MLP ──► 12 salidas por cultivo

- El conjunto se trata como un CONJUNTO (invariante al orden de los miembros) y su tamaño puede
  cambiar: se entrena con 10-25 miembros y en producción puede recibir los 51 del operativo.
- Física primero: el centro de los cuantiles parte del oráculo sobre la climatología (B0) y la red
  aprende cuánto y cuándo moverse de ahí (ENSO, humedad al sembrar, habilidad del pronóstico).
- ~0,56 M de parámetros por red: en CPU, un conjunto de 5 redes puntúa 23 cultivos con 51 miembros
  en ~30 ms.
"""

from collections.abc import Callable
from dataclasses import dataclass

import torch
from torch import Tensor, nn
from torch.nn import functional as F

from surco_planificador.caracteristicas import PENTADA, PENTADAS_PRONOSTICO


@dataclass(frozen=True)
class ConfigRed:
    historia_in: int
    pronostico_in: int
    clim_in: int
    estatico_in: int
    cultivo_in: int
    prior_in: int
    canales_historia: int = 64
    dilataciones: tuple[int, ...] = (1, 2, 4, 8, 16, 32)
    canales_pronostico: int = 48
    dim_miembro: int = 96
    dim_conjunto: int = 128
    canales_climatologia: int = 32
    oculto_estatico: int = 128
    embebido: int = 256
    oculto_cabeza: int = 192
    dropout: float = 0.1


class BloqueResidual(nn.Module):
    def __init__(self, canales: int, dilatacion: int, dropout: float) -> None:
        super().__init__()
        self.conv1 = nn.Conv1d(canales, canales, 3, padding=dilatacion, dilation=dilatacion)
        self.conv2 = nn.Conv1d(canales, canales, 1)
        self.norm = nn.GroupNorm(8, canales)
        self.drop = nn.Dropout(dropout)

    def forward(self, x: Tensor) -> Tensor:
        return x + self.drop(self.conv2(F.gelu(self.norm(self.conv1(x)))))


class PoolAtencional(nn.Module):
    """Pondera los pasos de tiempo: qué días del último año importan."""

    def __init__(self, canales: int) -> None:
        super().__init__()
        self.puntaje = nn.Conv1d(canales, 1, 1)

    def forward(self, x: Tensor) -> Tensor:
        return (x * torch.softmax(self.puntaje(x), dim=-1)).sum(-1)


class CodificadorConjunto(nn.Module):
    """Cada miembro por la misma CNN; luego media, desviación y atención sobre los miembros."""

    def __init__(self, cfg: ConfigRed) -> None:
        super().__init__()
        c = cfg.canales_pronostico
        self.entrada = nn.Conv1d(cfg.pronostico_in, c, 3, padding=1)
        self.bloques = nn.Sequential(*(BloqueResidual(c, d, cfg.dropout) for d in (1, 2, 4)))
        self.mensual = nn.AvgPool1d(PENTADA)  # 35 péntadas -> 7 "meses" de 25 días
        self.miembro = nn.Sequential(nn.Linear(c * (PENTADAS_PRONOSTICO // PENTADA), cfg.dim_miembro), nn.GELU())
        self.atencion = nn.Linear(cfg.dim_miembro, 1)
        self.salida = nn.Sequential(nn.Linear(3 * cfg.dim_miembro, cfg.dim_conjunto), nn.GELU())

    def forward(self, x: Tensor) -> Tensor:
        """x [B, M, Ca, 35] -> [B, dim_conjunto]."""
        b, m = x.shape[0], x.shape[1]
        h = self.mensual(self.bloques(F.gelu(self.entrada(x.reshape(b * m, x.shape[2], x.shape[3])))))
        h = self.miembro(h.flatten(1)).reshape(b, m, -1)  # [B, M, D]
        media = h.mean(1)
        desv = torch.sqrt(((h - media[:, None]) ** 2).mean(1) + 1e-6)
        pesos = torch.softmax(self.atencion(h), dim=1)
        return self.salida(torch.cat([media, desv, (h * pesos).sum(1)], -1))


class PlanificadorEstacionalNet(nn.Module):
    def __init__(self, cfg: ConfigRed) -> None:
        super().__init__()
        hc, cc, e = cfg.canales_historia, cfg.canales_climatologia, cfg.embebido
        self.historia = nn.Sequential(nn.Conv1d(cfg.historia_in, hc, 1),
                                      *(BloqueResidual(hc, d, cfg.dropout) for d in cfg.dilataciones))  # fmt: skip
        self.pool_historia = PoolAtencional(hc)
        self.conjunto = CodificadorConjunto(cfg)
        self.clim = nn.Sequential(
            nn.Conv1d(cfg.clim_in, cc, 5, stride=2, padding=2), nn.GELU(),
            nn.Conv1d(cc, cc, 5, stride=2, padding=2), nn.GELU(),
            nn.Conv1d(cc, cc, 5, stride=2, padding=2), nn.GELU(),
        )  # fmt: skip
        self.estatico = nn.Sequential(nn.Linear(cfg.estatico_in, cfg.oculto_estatico), nn.GELU(),
                                      nn.Linear(cfg.oculto_estatico, cfg.oculto_estatico), nn.GELU())  # fmt: skip
        self.sitio = nn.Sequential(nn.Linear(hc + cfg.dim_conjunto + cc + cfg.oculto_estatico, e), nn.GELU(),
                                   nn.Dropout(cfg.dropout), nn.LayerNorm(e))  # fmt: skip
        self.cultivo = nn.Sequential(nn.Linear(cfg.cultivo_in, e), nn.GELU(), nn.Linear(e, 2 * e))
        self.tronco = nn.Sequential(
            nn.Linear(e + cfg.prior_in, cfg.oculto_cabeza), nn.GELU(), nn.Dropout(cfg.dropout),
            nn.Linear(cfg.oculto_cabeza, cfg.oculto_cabeza), nn.GELU(),
        )  # fmt: skip
        self.cabeza = nn.Linear(cfg.oculto_cabeza, 12)
        nn.init.zeros_(self.cabeza.weight)  # arranca en el prior B0: la red aprende la anomalía
        nn.init.zeros_(self.cabeza.bias)

    def forward(self, historia: Tensor, pronostico: Tensor, clim: Tensor, estatico: Tensor,
                cultivos: Tensor, prior: Tensor) -> Tensor:
        """historia [B, Ch, 365], pronostico [B, M, Ca, 35], clim [B, Cc, 73], estatico [B, Fs],
        cultivos [C, Fk], prior [B, C, 14] -> [B, C, 12] (orden de caracteristicas.SALIDAS_RED)."""
        h = self.pool_historia(self.historia(historia))
        sitio = self.sitio(torch.cat([h, self.conjunto(pronostico), self.clim(clim).mean(-1),
                                      self.estatico(estatico)], -1))  # [B, E]  # fmt: skip
        gamma, beta = self.cultivo(cultivos).chunk(2, dim=-1)  # [C, E] (FiLM)
        cruzado = sitio[:, None, :] * (1 + gamma[None]) + beta[None]
        crudo = self.cabeza(self.tronco(torch.cat([cruzado, prior], -1)))  # [B, C, 12]
        logit_b0 = torch.logit(prior[..., 0:1].clamp(0.02, 0.98))
        logit_b0_riego = torch.logit(prior[..., 5:6].clamp(0.02, 0.98))
        return torch.cat([
            _ordenados(crudo[..., 0:3], lambda t: torch.sigmoid(t + logit_b0)),
            crudo[..., 3:4] + torch.logit(prior[..., 1:2].clamp(0.05, 0.95)),
            _ordenados(crudo[..., 4:7], lambda t: t + prior[..., 2:3]),
            torch.sigmoid(crudo[..., 7:9] + torch.logit(prior[..., 3:5].clamp(0.02, 0.98))),
            _ordenados(crudo[..., 9:12], lambda t: torch.sigmoid(t + logit_b0_riego)),
        ], -1)  # fmt: skip


def _ordenados(crudo: Tensor, centro: Callable[[Tensor], Tensor]) -> Tensor:
    """p10 <= p50 <= p90 por construcción: la mediana más dos brechas no negativas."""
    mitad = centro(crudo[..., 1:2])
    return torch.cat([mitad - F.softplus(crudo[..., 0:1]) * 0.1, mitad,
                      mitad + F.softplus(crudo[..., 2:3]) * 0.1], -1)  # fmt: skip


class Conjunto(nn.Module):
    """Promedio de varias redes (conjunto profundo). Exporta probabilidades, no logits."""

    def __init__(self, redes: list[nn.Module]) -> None:
        super().__init__()
        self.redes = nn.ModuleList(redes)

    def forward(self, historia: Tensor, pronostico: Tensor, clim: Tensor, estatico: Tensor,
                cultivos: Tensor, prior: Tensor) -> Tensor:
        salidas = torch.stack([r(historia, pronostico, clim, estatico, cultivos, prior) for r in self.redes])
        prob = torch.sigmoid(salidas[..., 3:4]).mean(0)
        media = salidas.mean(0)
        return torch.cat([media[..., :3], prob, media[..., 4:]], -1)
