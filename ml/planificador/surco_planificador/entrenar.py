"""Etapas `ajustar` y `entrenar`: reanudable, bf16 en la GPU, parada temprana, conjunto de semillas."""

import json
import logging
import math
import platform
import random
import time
from dataclasses import asdict, dataclass, replace
from pathlib import Path
from typing import Any, cast

import numpy as np
import numpy.typing as npt
import torch
from torch import Tensor
from torch.nn import functional as F

from surco_planificador.caracteristicas import PRIOR
from surco_planificador.config import cargar_yaml
from surco_planificador.datos_gpu import DatosDispositivo, Muestras
from surco_planificador.modelo import ConfigRed, PlanificadorEstacionalNet

log = logging.getLogger(__name__)
CUANTILES = (0.1, 0.5, 0.9)


@dataclass(frozen=True)
class ConfigOptim:
    tamano_lote: int = 1024
    lr: float = 1e-3
    decaimiento_pesos: float = 1e-2
    fraccion_calentamiento: float = 0.1
    recorte_gradiente: float = 1.0
    max_epocas: int = 40
    paciencia: int = 6
    miembros_min: int = 10


@dataclass(frozen=True)
class PesosPerdida:
    rendimiento: float = 1.0
    rendimiento_riego: float = 0.5
    exito: float = 0.5
    riego: float = 0.5
    estres: float = 0.2


@dataclass(frozen=True)
class ConfigEjecucion:
    precision: str = "bf16"
    compilar: str = "auto"
    semilla: int = 7
    deterministico_final: bool = True
    semillas_conjunto: tuple[int, ...] = (7,)
    pronostico_en: str = "auto"


@dataclass(frozen=True)
class ConfigEntrenamiento:
    modelo: dict[str, Any]
    optim: ConfigOptim
    perdida: PesosPerdida
    ejecucion: ConfigEjecucion
    ajuste: dict[str, Any]


def cargar_config(ruta: Path) -> ConfigEntrenamiento:
    r = cargar_yaml(ruta)
    modelo = dict(r["modelo"])
    modelo["dilataciones"] = tuple(modelo["dilataciones"])
    ejec = dict(r["ejecucion"])
    ejec["semillas_conjunto"] = tuple(ejec.get("semillas_conjunto", [ejec["semilla"]]))
    return ConfigEntrenamiento(modelo, ConfigOptim(**r["optimizacion"]), PesosPerdida(**r["pesos_perdida"]),
                               ConfigEjecucion(**ejec), r["ajuste"])  # fmt: skip


def pinball(pred: Tensor, obj: Tensor, mascara: Tensor) -> Tensor:
    """pred [..., 3] para CUANTILES, obj [...], mascara [...]."""
    q = torch.tensor(CUANTILES, device=pred.device, dtype=pred.dtype)
    d = obj[..., None] - pred
    return (torch.maximum(q * d, (q - 1) * d).mean(-1) * mascara).sum() / mascara.sum().clamp(min=1)


def perdida(out: Tensor, obj: Tensor, mascara: Tensor, w: PesosPerdida) -> Tensor:
    out, m = out.float(), mascara.float()

    def media(x: Tensor) -> Tensor:
        return (x * m).sum() / m.sum().clamp(min=1)

    return (w.rendimiento * pinball(out[..., 0:3], obj[..., 0], m)
            + w.exito * media(F.binary_cross_entropy_with_logits(out[..., 3], obj[..., 1], reduction="none"))
            + w.riego * pinball(out[..., 4:7], obj[..., 2], m)
            + w.estres * media(F.huber_loss(out[..., 7:9], obj[..., 3:5], delta=0.1, reduction="none").mean(-1))
            + w.rendimiento_riego * pinball(out[..., 9:12], obj[..., 5], m))  # fmt: skip


def sembrar(semilla: int, deterministico: bool) -> None:
    random.seed(semilla)
    np.random.seed(semilla)
    torch.manual_seed(semilla)
    torch.backends.cudnn.benchmark = not deterministico
    torch.backends.cudnn.deterministic = deterministico


def elegir_dispositivo() -> torch.device:
    if torch.cuda.is_available():
        torch.backends.cuda.matmul.allow_tf32 = True
        torch.backends.cudnn.allow_tf32 = True
        return torch.device("cuda")
    log.warning("sin CUDA: entrenar en CPU será muy lento")
    return torch.device("cpu")


def autocast(device: torch.device, precision: str) -> torch.autocast:
    activo = device.type == "cuda" and precision == "bf16" and torch.cuda.is_bf16_supported()
    return torch.autocast(device_type=device.type, dtype=torch.bfloat16, enabled=activo)


def compilar(modelo: torch.nn.Module, modo: str) -> torch.nn.Module:
    if modo == "off" or (modo == "auto" and platform.system() == "Windows"):
        return modelo  # torch.compile necesita Triton, que no está en Windows
    try:
        return cast("torch.nn.Module", torch.compile(modelo, dynamic=True))
    except Exception:
        log.warning("torch.compile no disponible; modo eager")
        return modelo


def construir_modelo(cfg: ConfigEntrenamiento, datos: DatosDispositivo, extra: dict[str, Any]
                     ) -> PlanificadorEstacionalNet:
    return PlanificadorEstacionalNet(ConfigRed(
        historia_in=datos.historia.shape[-1], pronostico_in=datos.anomalias.shape[-1],
        clim_in=datos.clim.shape[-1], estatico_in=datos.sitio.shape[-1] + datos.contexto.shape[-1],
        cultivo_in=datos.cultivos.shape[-1], prior_in=len(PRIOR), **{**cfg.modelo, **extra},
    ))  # fmt: skip


def _tensores(m: Muestras, ini: int, n: int, dev: torch.device) -> tuple[Tensor, Tensor]:
    return (torch.as_tensor(m.p[ini : ini + n], device=dev), torch.as_tensor(m.s[ini : ini + n], device=dev))


@torch.no_grad()
def perdida_eval(modelo: torch.nn.Module, datos: DatosDispositivo, m: Muestras, cfg: ConfigEntrenamiento,
                 dev: torch.device) -> float:
    modelo.eval()
    total, n = 0.0, 0
    for ini in range(0, len(m), 2048):
        p, s = _tensores(m, ini, 2048, dev)
        entradas, obj, mascara = datos.lote(p, s)
        with autocast(dev, cfg.ejecucion.precision):
            out = modelo(*entradas)
        total += float(perdida(out, obj, mascara, cfg.perdida)) * len(p)
        n += len(p)
    return total / max(n, 1)


@torch.no_grad()
def predecir(modelo: torch.nn.Module, datos: DatosDispositivo, m: Muestras, dev: torch.device,
             precision: str) -> npt.NDArray[np.float32]:
    """[N, C, 12] en el orden de caracteristicas.SALIDAS_RED, con todos los miembros."""
    modelo.eval()
    trozos = []
    for ini in range(0, len(m), 2048):
        p, s = _tensores(m, ini, 2048, dev)
        entradas, _, _ = datos.lote(p, s)
        with autocast(dev, precision):
            trozos.append(modelo(*entradas).float().cpu().numpy())
    return np.concatenate(trozos).astype(np.float32)


@dataclass
class Resultado:
    mejor_val: float
    mejor_epoca: int
    epocas: int
    segundos: float


class Entrenador:
    """Una corrida, reanudable desde `carpeta/ultimo.pt` tras un corte de luz."""

    def __init__(self, cfg: ConfigEntrenamiento, datos: DatosDispositivo, dev: torch.device, carpeta: Path,
                 extra: dict[str, Any] | None = None, semilla: int | None = None) -> None:
        self.cfg, self.datos, self.dev, self.carpeta = cfg, datos, dev, carpeta
        self.extra = extra or {}
        self.semilla = cfg.ejecucion.semilla if semilla is None else semilla
        carpeta.mkdir(parents=True, exist_ok=True)

    def ajustar(self, entreno: Muestras, val: Muestras | None, epocas: int | None = None) -> Resultado:
        cfg, o = self.cfg, self.cfg.optim
        sembrar(self.semilla, cfg.ejecucion.deterministico_final)
        modelo = construir_modelo(cfg, self.datos, self.extra).to(self.dev)
        max_epocas = epocas or o.max_epocas
        self._pasos = max_epocas * math.ceil(len(entreno) / o.tamano_lote)
        optim = torch.optim.AdamW(modelo.parameters(), lr=o.lr, weight_decay=o.decaimiento_pesos)
        plan = torch.optim.lr_scheduler.OneCycleLR(optim, max_lr=o.lr, total_steps=self._pasos,
                                                   pct_start=o.fraccion_calentamiento)  # fmt: skip
        estado = {"epoca": 0, "mejor_val": math.inf, "mejor_epoca": 0, "malas": 0}
        if self._reanudar(modelo, optim, plan, estado):
            log.info("%s reanudada en la época %d", self.carpeta.name, estado["epoca"])
        compilado = compilar(modelo, cfg.ejecucion.compilar)
        inicio = time.monotonic()
        while estado["epoca"] < max_epocas and estado["malas"] < o.paciencia:
            perdida_ent = self._epoca(compilado, optim, plan, entreno, int(estado["epoca"]))
            estado["epoca"] += 1
            perdida_val = perdida_eval(compilado, self.datos, val, cfg, self.dev) if val else perdida_ent
            if perdida_val < estado["mejor_val"] - 1e-5:
                estado.update(mejor_val=perdida_val, mejor_epoca=estado["epoca"], malas=0)
                torch.save(modelo.state_dict(), self.carpeta / "mejor.pt")
            elif val is not None:
                estado["malas"] += 1
            self._registrar(estado, perdida_ent, perdida_val)
            self._guardar(modelo, optim, plan, estado)
        if val is None:  # un reajuste conserva sus pesos finales
            torch.save(modelo.state_dict(), self.carpeta / "mejor.pt")
        return Resultado(float(estado["mejor_val"]), int(estado["mejor_epoca"]), int(estado["epoca"]),
                         time.monotonic() - inicio)  # fmt: skip

    def cargar_mejor(self) -> PlanificadorEstacionalNet:
        modelo = construir_modelo(self.cfg, self.datos, self.extra).to(self.dev)
        modelo.load_state_dict(torch.load(self.carpeta / "mejor.pt", map_location=self.dev))
        return modelo

    def _epoca(self, modelo: torch.nn.Module, optim: torch.optim.Optimizer,
               plan: torch.optim.lr_scheduler.LRScheduler, entreno: Muestras, epoca: int) -> float:
        modelo.train()
        gen = torch.Generator().manual_seed(self.semilla * 1000 + epoca)
        orden = torch.randperm(len(entreno), generator=gen).numpy()
        bs, total, n = self.cfg.optim.tamano_lote, 0.0, 0
        m_max = self.datos.miembros
        m_min = min(self.cfg.optim.miembros_min, m_max)
        for ini in range(0, len(orden), bs):
            idx = orden[ini : ini + bs]
            if len(idx) < 2:
                continue
            p = torch.as_tensor(entreno.p[idx], device=self.dev)
            s = torch.as_tensor(entreno.s[idx], device=self.dev)
            miembros = int(torch.randint(m_min, m_max + 1, (1,), generator=gen))
            entradas, obj, mascara = self.datos.lote(p, s, miembros, gen)
            with autocast(self.dev, self.cfg.ejecucion.precision):
                out = modelo(*entradas)
            loss = perdida(out, obj, mascara, self.cfg.perdida)
            optim.zero_grad(set_to_none=True)
            loss.backward()
            torch.nn.utils.clip_grad_norm_(modelo.parameters(), self.cfg.optim.recorte_gradiente)
            optim.step()
            if plan.last_epoch < self._pasos - 1:
                plan.step()
            total += float(loss.detach()) * len(idx)
            n += len(idx)
        return total / max(n, 1)

    def _registrar(self, estado: dict[str, Any], ent: float, val: float) -> None:
        log.info("%s época %d entrenamiento %.5f validación %.5f", self.carpeta.name, estado["epoca"], ent, val)
        with (self.carpeta / "historial.jsonl").open("a", encoding="utf-8") as f:
            f.write(json.dumps({"epoca": estado["epoca"], "entrenamiento": round(ent, 5),
                                "validacion": round(val, 5), "mejor_epoca": estado["mejor_epoca"]}) + "\n")  # fmt: skip

    def _guardar(self, modelo: torch.nn.Module, optim: torch.optim.Optimizer,
                 plan: torch.optim.lr_scheduler.LRScheduler, estado: dict[str, Any]) -> None:
        tmp = self.carpeta / "ultimo.pt.part"
        torch.save({"modelo": modelo.state_dict(), "optim": optim.state_dict(), "plan": plan.state_dict(),
                    "estado": estado}, tmp)  # fmt: skip
        tmp.replace(self.carpeta / "ultimo.pt")

    def _reanudar(self, modelo: torch.nn.Module, optim: torch.optim.Optimizer,
                  plan: torch.optim.lr_scheduler.LRScheduler, estado: dict[str, Any]) -> bool:
        ruta = self.carpeta / "ultimo.pt"
        if not ruta.exists():
            return False
        g = torch.load(ruta, map_location=self.dev)
        modelo.load_state_dict(g["modelo"])
        optim.load_state_dict(g["optim"])
        plan.load_state_dict(g["plan"])
        estado.update(g["estado"])
        return True


def muestrear_prueba(espacio: dict[str, Any], rng: random.Random) -> tuple[dict[str, Any], dict[str, Any]]:
    bajo, alto = espacio["lr"]
    modelo = {"canales_historia": rng.choice(espacio["canales_historia"]),
              "canales_pronostico": rng.choice(espacio["canales_pronostico"]),
              "dropout": round(rng.uniform(*espacio["dropout"]), 3)}  # fmt: skip
    optim = {"lr": math.exp(rng.uniform(math.log(bajo), math.log(alto))),
             "tamano_lote": rng.choice(espacio["tamano_lote"])}  # fmt: skip
    return modelo, optim


def buscar(cfg: ConfigEntrenamiento, datos: DatosDispositivo, dev: torch.device, entreno: Muestras,
           val: Muestras, carpeta: Path) -> dict[str, Any]:
    """Búsqueda aleatoria; las pruebas terminadas se releen de pruebas.jsonl, así se reanuda."""
    carpeta.mkdir(parents=True, exist_ok=True)
    registro = carpeta / "pruebas.jsonl"
    hechas = [json.loads(x) for x in registro.read_text(encoding="utf-8").splitlines()] if registro.exists() else []
    rng = random.Random(cfg.ejecucion.semilla)
    pruebas = [muestrear_prueba(cfg.ajuste["espacio"], rng) for _ in range(int(cfg.ajuste["pruebas"]))]
    for i, (mod, opt) in enumerate(pruebas):
        if any(h["prueba"] == i for h in hechas):
            continue
        cfg_i = replace(cfg, optim=replace(cfg.optim, max_epocas=int(cfg.ajuste["epocas"]), **opt),
                        ejecucion=replace(cfg.ejecucion, deterministico_final=False))  # fmt: skip
        r = Entrenador(cfg_i, datos, dev, carpeta / f"prueba_{i:02d}", mod).ajustar(entreno, val)
        fila = {"prueba": i, "modelo": mod, "optim": opt, **asdict(r)}
        hechas.append(fila)
        with registro.open("a", encoding="utf-8") as f:
            f.write(json.dumps(fila) + "\n")
        log.info("prueba %d/%d validación %.5f", i + 1, len(pruebas), r.mejor_val)
    mejor = min(hechas, key=lambda h: h["mejor_val"])
    log.info("mejor prueba %d: validación %.5f", mejor["prueba"], mejor["mejor_val"])
    return mejor
