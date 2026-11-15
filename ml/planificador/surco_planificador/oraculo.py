"""Oráculo agronómico: qué tan bien le va a un cultivo con un clima diario dado.

No existe un dataset público de "sembré c en la fecha d en este lote y coseché y". Las etiquetas
salen de modelos FAO publicados corridos sobre el clima que de verdad ocurrió, usando la ficha de
cada cultivo del catálogo surco_agro fase por fase:

- Agua: balance hídrico FAO-56 (Kc y p por fase, TAW del suelo) y respuesta FAO-33 con Ky.
  Sin Kc o p en la ficha (ñame), el agua se evalúa con los trapecios de lluvia por fase.
- Temperatura: trapecio de cada fase día a día. Días sobre la crítica de la fase (floración)
  y heladas (Tmin <= 0 °C) restan aparte.
- Exceso de lluvia: lado alto del trapecio de lluvia de cada fase (encharcamiento, enfermedades).
- pH del suelo (0-30 cm) contra el trapecio del cultivo.

rendimiento_riego = temperatura × calor crítico × heladas × exceso × pH (con riego, el agua no limita).
rendimiento (secano) = agua × rendimiento_riego.

La red aprende a anticipar este oráculo SIN ver el clima futuro: ese es su valor. Por eso el modelo
es tan bueno como las fichas del catálogo, y no está calibrado contra cosechas reales.
"""

from dataclasses import dataclass

import numpy as np
import numpy.typing as npt

from surco_planificador.catalogo import FichaCultivo, Trapecio

A = npt.NDArray[np.float32]
DIAS_CRITICOS_A_CERO = 10.0  # 10 días sobre la temperatura crítica de la fase anulan ese factor
HELADA_C = 0.0
PERDIDA_POR_HELADA = 0.25  # cada helada resta un 25 %


@dataclass(frozen=True)
class CicloClima:
    """[N, días del ciclo] cada uno, más el mes previo a la siembra para arrancar el balance."""

    t: A
    tmax: A
    tmin: A
    lluvia: A
    et0: A
    lluvia_previa: A  # [N, días previos]
    et0_previa: A


def trapecio(x: A, a: A | float, b: A | float, c: A | float, d: A | float) -> A:
    sube = (x - a) / np.maximum(np.asarray(b) - a, 1e-6)
    baja = (d - x) / np.maximum(np.asarray(d) - c, 1e-6)
    return np.clip(np.minimum(sube, baja), 0.0, 1.0).astype(np.float32)


def lado_bajo(x: A, t: Trapecio) -> A:
    return np.clip((x - t.a) / max(t.b - t.a, 1e-6), 0.0, 1.0).astype(np.float32)


def lado_alto(x: A, t: Trapecio) -> A:
    return np.clip((t.d - x) / max(t.d - t.c, 1e-6), 0.0, 1.0).astype(np.float32)


def balance_hidrico(lluvia: A, etc: A, taw: float, p_dia: npt.NDArray[np.float64], agotamiento0: A,
                    regar: bool) -> tuple[A, A, A, A]:
    """FAO-56 Ec. 84-85 con pasos diarios. Devuelve ETa, ETc, riego (mm) y el agotamiento final.

    La lluvia por encima del déficit se va como escorrentía o percolación. Con `regar`, cada vez
    que el agotamiento supera RAW se llena la zona radicular a capacidad de campo.
    """
    agot = agotamiento0.astype(np.float32, copy=True)
    eta = np.zeros(lluvia.shape[0], np.float32)
    riego = np.zeros(lluvia.shape[0], np.float32)
    for dia in range(lluvia.shape[1]):
        raw = float(p_dia[min(dia, len(p_dia) - 1)]) * taw
        if regar:
            aplicado = np.where(agot > raw, agot, 0.0)
            riego += aplicado
            agot -= aplicado
        ks = np.where(agot <= raw, 1.0, np.clip((taw - agot) / max(taw - raw, 1e-6), 0.0, 1.0))
        real = ks * etc[:, dia]
        eta += real
        agot = np.clip(agot - lluvia[:, dia] + real, 0.0, taw)
    return eta, etc.sum(axis=1), riego, agot


class Oraculo:
    """Precalcula los parámetros diarios de una ficha; `simular` es vectorizado en N climas."""

    def __init__(self, ficha: FichaCultivo, umbral_exito: float) -> None:
        self.ficha = ficha
        self.umbral = umbral_exito
        self.d = ficha.diarios()
        self.fases = ficha.limites_fases()

    def simular(self, clima: CicloClima, taw: float, ph_suelo: float) -> A:
        """-> [N, 6] en el orden de caracteristicas.ETIQUETAS."""
        f, d, L = self.ficha, self.d, self.ficha.ciclo_dias
        t, tmax, tmin, lluvia = clima.t[:, :L], clima.tmax[:, :L], clima.tmin[:, :L], clima.lluvia[:, :L]
        temperatura = trapecio(t, d["ta"], d["tb"], d["tc"], d["td"]).mean(axis=1)
        dias_calor = (t > d["tc"]).sum(axis=1).astype(np.float32)
        dias_frio = (t < d["tb"]).sum(axis=1).astype(np.float32)
        criticos = (tmax > np.where(np.isnan(d["t_critica"]), np.inf, d["t_critica"])).sum(axis=1)
        calor_critico = np.clip(1 - criticos / DIAS_CRITICOS_A_CERO, 0, 1)
        heladas = np.clip(1 - PERDIDA_POR_HELADA * (tmin <= HELADA_C).sum(axis=1), 0, 1)
        exceso = np.zeros(len(t), np.float32)
        agua_sin_balance = np.zeros(len(t), np.float32)
        deficit = np.zeros(len(t), np.float32)
        for (a, b), fase in zip(self.fases, f.fases, strict=True):
            lluvia_fase = lluvia[:, a:b].sum(axis=1)
            alto = lado_alto(lluvia_fase, fase.lluvia)
            exceso += fase.fraccion * (alto if fase.exceso_perjudicial else 1 - 0.5 * (1 - alto))
            agua_sin_balance += fase.fraccion * lado_bajo(lluvia_fase, fase.lluvia)
            deficit += np.maximum(fase.lluvia.b - lluvia_fase, 0.0)
        if f.balance_hidrico:
            agua, riego = self._agua_fao56(clima, taw)
        else:
            agua, riego = agua_sin_balance, deficit
        ph = float(trapecio(np.array([ph_suelo], np.float32), f.ph.a, f.ph.b, f.ph.c, f.ph.d)[0])
        con_riego = temperatura * calor_critico * heladas * exceso * ph
        secano = agua * con_riego
        return np.stack([secano, (secano >= self.umbral).astype(np.float32), riego, dias_calor, dias_frio,
                         con_riego], axis=1).astype(np.float32)  # fmt: skip

    def _agua_fao56(self, clima: CicloClima, taw: float) -> tuple[A, A]:
        f, d, L = self.ficha, self.d, self.ficha.ciclo_dias
        inicial = np.full(clima.t.shape[0], 0.5 * taw, np.float32)
        previo = balance_hidrico(clima.lluvia_previa, clima.et0_previa * float(d["kc"][0]), taw,
                                 d["p"][:1], inicial, regar=False)  # fmt: skip
        etc = clima.et0[:, :L] * d["kc"][None, :].astype(np.float32)
        eta, etc_total, _, _ = balance_hidrico(clima.lluvia[:, :L], etc, taw, d["p"], previo[3], regar=False)
        _, _, riego, _ = balance_hidrico(clima.lluvia[:, :L], etc, taw, d["p"], previo[3], regar=True)
        relacion = np.where(etc_total > 0, eta / np.maximum(etc_total, 1e-6), 1.0)
        return np.clip(1 - f.ky * (1 - relacion), 0.0, 1.0).astype(np.float32), riego
