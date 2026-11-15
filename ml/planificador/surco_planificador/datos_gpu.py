"""Tensores de entrenamiento en la GPU y el armado de lotes por índice (sin cuello de botella de DataLoader).

Memoria aproximada con ~200 puntos, 528 inicios, 25 miembros y 23 cultivos: historia 0,15 GB,
climatología 0,08 GB, anomalías del pronóstico 0,9 GB (fp16), objetivos 0,4 GB y priors 0,9 GB.
Cabe de sobra en los 16 GB de la RTX 5060 Ti; con `pronostico_en: cpu` las anomalías quedan en
RAM fijada y viajan por lote.
"""

from dataclasses import dataclass

import numpy as np
import numpy.typing as npt
import torch
from torch import Tensor

from surco_planificador.caracteristicas import (
    CANALES_ANOMALIA,
    DIAS_ADELANTE,
    DIAS_HISTORIA,
    PENTADA,
    PENTADAS_GUARDADAS,
    PENTADAS_PRONOSTICO,
    PRIOR_B0,
    Estandarizador,
    Normalizacion,
    a_objetivos,
    oni_conocido,
    transformar_clim,
    transformar_historia,
    vector_contexto,
)
from surco_planificador.catalogo import FichaCultivo, matriz_fichas
from surco_planificador.config import Anios, ConfigPipeline
from surco_planificador.datos import DatosLimpios
from surco_planificador.etiquetas import DisenoSiembras, Etiquetas
from surco_planificador.limpieza.climatologia import indice_doy

F32 = npt.NDArray[np.float32]
N_B0 = len(PRIOR_B0)


@dataclass(frozen=True)
class Muestras:
    p: npt.NDArray[np.int64]
    s: npt.NDArray[np.int64]

    @classmethod
    def de_mascara(cls, m: npt.NDArray[np.bool_]) -> "Muestras":
        p, s = np.nonzero(m)
        return cls(p.astype(np.int64), s.astype(np.int64))

    def __len__(self) -> int:
        return len(self.p)

    def primeras(self, n: int) -> "Muestras":
        return Muestras(self.p[:n], self.s[:n])


def anios_siembra(dis: DisenoSiembras) -> npt.NDArray[np.int64]:
    return np.array([f.year for f in dis.fechas()], dtype=np.int64)


def divisiones(datos: DatosLimpios, etq: Etiquetas, dis: DisenoSiembras, cfg: ConfigPipeline,
               reservar: bool) -> dict[str, Muestras]:
    """Por año de siembra; con `reservar`, los departamentos reservados solo se prueban."""
    anios = anios_siembra(dis)
    reservados = np.isin(datos.departamentos, cfg.departamentos_reservados)
    vistos = ~reservados if reservar else np.ones_like(reservados)
    probados = reservados if reservar else vistos
    alguno = etq.valido.any(axis=-1)

    def m(puntos: npt.NDArray[np.bool_], *spans: Anios) -> Muestras:
        en = np.zeros_like(anios, dtype=bool)
        for a in spans:
            en |= (anios >= a.primero) & (anios <= a.ultimo)
        return Muestras.de_mascara(alguno & puntos[:, None] & en[None, :])

    return {"entrenamiento": m(vistos, cfg.entrenamiento), "validacion": m(vistos, cfg.validacion),
            "entrenamiento+validacion": m(vistos, cfg.entrenamiento, cfg.validacion),
            "prueba": m(probados, cfg.prueba)}  # fmt: skip


def contextos(datos: DatosLimpios, dis: DisenoSiembras) -> F32:
    """[S, len(CONTEXTO)]: igual para todos los puntos en una fecha de siembra."""
    oni = datos.mapa_oni()
    out = []
    for fecha, o in zip(dis.fechas(), np.tile(dis.desplazamientos, len(dis.inicios)), strict=True):
        ultimo, tendencia = oni_conocido(oni, fecha)
        out.append(vector_contexto(ultimo, tendencia, fecha, int(o)))
    return np.array(out, np.float32)


def ajustar_normalizacion(datos: DatosLimpios, anom: npt.NDArray[np.float16], ctx: F32, entreno: Muestras,
                          dis: DisenoSiembras, fichas: list[FichaCultivo], cfg: ConfigPipeline) -> Normalizacion:
    """Solo con lo que existía en los años de entrenamiento y en sus puntos."""
    pts = np.unique(entreno.p)
    ultimo = datos.indice(dis.fechas()[int(entreno.s.max())])
    hist = transformar_historia(datos.clima[pts, :ultimo])
    hist[~datos.valido[pts, :ultimo]] = np.nan
    anios_ok = datos.anios_clim <= cfg.entrenamiento.ultimo
    inicios_ok = np.unique(dis.inicio_de(entreno.s))
    suma, suma2, n = np.zeros(len(CANALES_ANOMALIA)), np.zeros(len(CANALES_ANOMALIA)), 0
    for p in pts:  # por punto: no hace falta subir las anomalías completas a fp32
        a = np.asarray(anom[p][inicios_ok], np.float64).reshape(-1, len(CANALES_ANOMALIA))
        suma, suma2, n = suma + a.sum(0), suma2 + (a**2).sum(0), n + len(a)
    media = suma / n
    anomalia = Estandarizador(media.astype(np.float32),
                              np.sqrt(np.maximum(suma2 / n - media**2, 1e-6)).astype(np.float32))  # fmt: skip
    return Normalizacion(
        historia=Estandarizador.ajustar(hist, (0, 1)),
        clim=Estandarizador.ajustar(transformar_clim(datos.clim[pts][:, anios_ok]), (0, 1, 2)),
        anomalia=anomalia,
        estatico=Estandarizador.ajustar(np.concatenate([datos.sitio[entreno.p], ctx[entreno.s]], 1), (0,)),
        cultivos=Estandarizador.ajustar(matriz_fichas(fichas), (0,)),
    )


@dataclass
class DatosDispositivo:
    historia: Tensor  # [P, D, Ch] normalizada
    clim: Tensor  # [P, Y, 366, Cc] normalizada
    anomalias: Tensor  # [P, I, M, 42, Ca] normalizadas, fp16 (GPU o RAM fijada)
    sitio: Tensor  # [P, Fs]
    contexto: Tensor  # [S, Fc]
    est_media: Tensor
    est_desv: Tensor
    cultivos: Tensor  # [C, Fk] normalizados
    siembra: Tensor  # [S] índice de día
    anio_clim: Tensor  # [S]
    doy: Tensor  # [S]
    inicio: Tensor  # [S]
    pentada0: Tensor  # [S] primera péntada del pronóstico desde la siembra (k + 1)
    objetivos: Tensor  # [P, S, C, 6]
    prior: Tensor  # [P, S, C, 14]
    mascara: Tensor  # [P, S, C]
    usar_pronostico: bool = True

    @property
    def miembros(self) -> int:
        return int(self.anomalias.shape[2])

    def lote(self, p: Tensor, s: Tensor, miembros: int | None = None, gen: torch.Generator | None = None
             ) -> tuple[tuple[Tensor, ...], Tensor, Tensor]:
        """(historia, pronostico, clim, estatico, cultivos, prior), objetivos, máscara."""
        dev = self.historia.device
        dias = self.siembra[s][:, None] + torch.arange(-DIAS_HISTORIA, 0, device=dev)[None, :]
        historia = self.historia[p[:, None], dias].transpose(1, 2)
        doy = (self.doy[s][:, None] + torch.arange(DIAS_ADELANTE, device=dev)[None, :]) % 366
        clim = self.clim[p[:, None], self.anio_clim[s][:, None], doy]  # [B, 365, Cc]
        clim = clim.reshape(len(p), -1, PENTADA, clim.shape[-1]).mean(2).transpose(1, 2)
        estatico = (torch.cat([self.sitio[p], self.contexto[s]], 1) - self.est_media) / self.est_desv
        prior = self.prior[p, s]
        pron = self._pronostico(p, s, miembros, gen)
        if not self.usar_pronostico:
            pron = torch.zeros_like(pron)
            prior = torch.cat([prior[..., :N_B0], torch.zeros_like(prior[..., N_B0:])], -1)
        entradas = (historia, pron, clim, estatico, self.cultivos, prior)
        return entradas, self.objetivos[p, s], self.mascara[p, s]

    def _pronostico(self, p: Tensor, s: Tensor, miembros: int | None, gen: torch.Generator | None) -> Tensor:
        dev_a = self.anomalias.device
        pa, sa = p.to(dev_a), s.to(dev_a)
        j = self.pentada0.to(dev_a)[sa][:, None] + torch.arange(PENTADAS_PRONOSTICO, device=dev_a)[None, :]
        x = self.anomalias[pa[:, None], self.inicio.to(dev_a)[sa][:, None], :, j]  # [B, 35, M, Ca]
        if miembros is not None and miembros < x.shape[2]:
            elegidos = torch.randperm(x.shape[2], generator=gen)[:miembros].to(dev_a)
            x = x[:, :, elegidos]
        return x.permute(0, 2, 3, 1).to(self.historia.device, non_blocking=True).float()  # [B, M, Ca, 35]


def pentada_de_siembra(desplazamiento: npt.NDArray[np.int64]) -> npt.NDArray[np.int64]:
    """La péntada j cubre los días 1+5j..5+5j del pronóstico; sembrar el día o empieza en (o-1)/5."""
    if np.any((desplazamiento - 1) % PENTADA != 0):
        raise ValueError("los desplazamientos de siembra deben ser 1 + 5k (6, 11, 16...)")
    j = (desplazamiento - 1) // PENTADA
    if np.any(j + PENTADAS_PRONOSTICO > PENTADAS_GUARDADAS):
        raise ValueError("un desplazamiento deja menos de 175 días de pronóstico")
    return j


def _anomalias_normalizadas(anom: npt.NDArray[np.float16], norm: Normalizacion) -> Tensor:
    out = torch.empty(anom.shape, dtype=torch.float16)
    for p in range(anom.shape[0]):
        out[p] = torch.from_numpy(norm.anomalia.aplicar(np.asarray(anom[p], np.float32)).astype(np.float16))
    return out


def a_dispositivo(datos: DatosLimpios, etq: Etiquetas, anom: npt.NDArray[np.float16], ctx: F32,
                  dis: DisenoSiembras, fichas: list[FichaCultivo], norm: Normalizacion,
                  device: torch.device, pronostico_en: str = "auto") -> DatosDispositivo:
    ciclos = np.array([f.ciclo_dias for f in fichas], np.float32)
    fechas = dis.fechas()

    def t(x: npt.ArrayLike, dtype: torch.dtype = torch.float32) -> Tensor:
        return torch.as_tensor(np.ascontiguousarray(x), dtype=dtype, device=device)

    a = _anomalias_normalizadas(anom, norm)
    en_gpu = device.type == "cuda" and (pronostico_en == "gpu" or (
        pronostico_en == "auto" and a.numel() * 2 < 0.4 * torch.cuda.get_device_properties(0).total_memory))
    a = a.to(device) if en_gpu or device.type == "cpu" else a.pin_memory()
    hist = norm.historia.aplicar(transformar_historia(datos.clima))
    hist[~datos.valido] = 0.0
    return DatosDispositivo(
        historia=t(hist), clim=t(norm.clim.aplicar(transformar_clim(datos.clim))),
        anomalias=a, sitio=t(datos.sitio), contexto=t(ctx), est_media=t(norm.estatico.media),
        est_desv=t(norm.estatico.desv), cultivos=t(norm.cultivos.aplicar(matriz_fichas(fichas))),
        siembra=t([datos.indice(f) for f in fechas], torch.long),
        anio_clim=t(np.clip([f.year - int(datos.anios_clim[0]) for f in fechas], 0, len(datos.anios_clim) - 1),
                    torch.long),
        doy=t([indice_doy(f) for f in fechas], torch.long),
        inicio=t(dis.inicio_de(np.arange(dis.n)), torch.long),
        pentada0=t(pentada_de_siembra(dis.desplazamiento_de(np.arange(dis.n))), torch.long),
        objetivos=t(np.nan_to_num(a_objetivos(etq.etiquetas, ciclos))),
        prior=t(np.nan_to_num(np.concatenate([a_objetivos(etq.b0, ciclos), etq.bf], -1))),
        mascara=t(etq.valido, torch.bool),
    )  # fmt: skip
