"""Las etapas en orden; cada una se salta si su salida ya existe (salvo `--forzar etapa`).

`python -m surco_planificador todo` corre todo; cualquier etapa corre sola. Un corte de luz o una
ventana cerrada no pierden trabajo: se vuelve a correr el mismo comando.
"""

import json
import logging
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, field, replace
from datetime import datetime
from pathlib import Path
from typing import Any

import numpy as np

from surco_planificador.catalogo import FichaCultivo, cargar_fichas, huella
from surco_planificador.config import ConfigPipeline, cargar_pipeline
from surco_planificador.construir import descargar, http_de, limpiar
from surco_planificador.datos import DatosLimpios
from surco_planificador.etiquetas import Etiquetas, construir_etiquetas, diseno
from surco_planificador.fuentes.c3s import descargar_inicio, ruta_inicio
from surco_planificador.pronostico import interpolar_todo
from surco_planificador.puntos import escribir_puntos, leer_puntos, seleccionar_puntos
from surco_planificador.rutas import Rutas

log = logging.getLogger(__name__)
ETAPAS = ("entorno", "puntos", "descargar", "pronosticos", "limpiar", "interpolar", "etiquetas",
          "ajustar", "entrenar", "ablacion", "evaluar", "reserva", "exportar")  # fmt: skip
TODO = tuple(e for e in ETAPAS if e != "puntos")


@dataclass
class Tuberia:
    rutas: Rutas
    corrida: str
    forzar: frozenset[str] = frozenset()
    cfg: ConfigPipeline = field(init=False)
    fichas: list[FichaCultivo] = field(init=False)
    fuera: dict[str, str] = field(init=False)
    origen_catalogo: str = field(init=False)

    def __post_init__(self) -> None:
        self.cfg = cargar_pipeline(self.rutas.configs / "pipeline.yaml")
        self.fichas, self.fuera, self.origen_catalogo = cargar_fichas(
            self.rutas.catalogo_json, self.rutas.configs / "ky_fao33.yaml", self.cfg.max_ciclo_dias, self.cfg.excluir)
        log.info("%d cultivos en el modelo: %s", len(self.fichas), ", ".join(f.id for f in self.fichas))

    @property
    def carpeta_corrida(self) -> Path:
        return self.rutas.corridas / self.corrida

    def correr(self, etapas: tuple[str, ...]) -> None:
        for e in etapas:
            log.info("=== etapa %s ===", e)
            getattr(self, f"etapa_{e}")()

    def _hecho(self, ruta: Path, etapa: str) -> bool:
        if ruta.exists() and etapa not in self.forzar:
            log.info("%s ya existe; se salta (use --forzar %s para rehacer)", ruta.name, etapa)
            return True
        return False

    # --- datos ----------------------------------------------------------------------------

    def etapa_entorno(self) -> None:
        from surco_planificador.entorno import revisar

        if not revisar():
            raise SystemExit("la GPU no sirve; arregle el entorno antes de entrenar")

    def etapa_puntos(self) -> None:
        if self._hecho(self.rutas.puntos_csv, "puntos"):
            return
        puntos = seleccionar_puntos(self.rutas.divipola_json, http_de(self.rutas), self.cfg.max_puntos, self.cfg.semilla)
        escribir_puntos(self.rutas.puntos_csv, puntos)
        log.info("%d puntos escritos", len(puntos))

    def _puntos(self) -> list[Any]:
        if not self.rutas.puntos_csv.exists():
            self.etapa_puntos()
        return leer_puntos(self.rutas.puntos_csv)

    def etapa_descargar(self) -> None:
        descargar(self.rutas, self._puntos(), self.cfg)

    def etapa_pronosticos(self) -> None:
        """Descarga los inicios de SEAS5 del CDS (horas o días según la cola). Reanudable."""
        pc, carpeta = self.cfg.pronostico, self.rutas.crudo / "c3s"
        pendientes = [i for i in pc.inicios() if not ruta_inicio(carpeta, *i).exists()]
        log.info("SEAS5: %d de %d inicios por descargar", len(pendientes), len(pc.inicios()))
        if not pendientes:
            return
        # El primero va solo: si faltan credenciales o la licencia, se sabe en segundos.
        try:
            descargar_inicio(pc, carpeta, *pendientes[0])
        except Exception as e:
            texto = str(e).lower()
            if "licence" in texto or "license" in texto:
                raise SystemExit(
                    "El CDS rechaza el pedido: falta aceptar la licencia del dataset. Entre a "
                    "https://cds.climate.copernicus.eu/datasets/seasonal-original-single-levels?tab=download"
                    " , acepte la licencia al final de la página y vuelva a correr el script.") from e
            if "401" in texto or "unauthorized" in texto or "key" in texto:
                raise SystemExit(f"El CDS no acepta la clave de .cdsapirc: {e}") from e
            raise

        fallidos: list[str] = []

        def uno(inicio: tuple[int, int]) -> None:
            try:
                descargar_inicio(pc, carpeta, *inicio)
            except Exception as e:  # un inicio fallido no detiene los demás
                log.error("C3S %d-%02d falló: %s", *inicio, e)
                fallidos.append(f"{inicio[0]}-{inicio[1]:02d}")

        with ThreadPoolExecutor(max_workers=pc.descargas_paralelas) as pool:
            list(pool.map(uno, pendientes[1:]))
        if fallidos:  # no se entrena con pronósticos incompletos
            raise SystemExit(f"{len(fallidos)} inicios de SEAS5 fallaron ({', '.join(sorted(fallidos)[:6])}...). "
                             "Vuelva a correr el script: solo se piden los que faltan.")

    def etapa_limpiar(self) -> None:
        if self._hecho(self.rutas.limpio / "datos.npz", "limpiar"):
            return
        datos, _ = limpiar(self.rutas, self._puntos(), self.cfg)
        datos.guardar(self.rutas.limpio)

    def etapa_interpolar(self) -> None:
        datos = DatosLimpios.cargar(self.rutas.limpio)
        interpolar_todo(self.cfg.pronostico, self.rutas.crudo / "c3s", self.rutas.pronosticos,
                        [(float(a), float(b)) for a, b in zip(datos.lat, datos.lon, strict=True)])  # fmt: skip

    def etapa_etiquetas(self) -> None:
        if self._hecho(self.rutas.etiquetas / "etiquetas.npz", "etiquetas"):
            return
        construir_etiquetas(DatosLimpios.cargar(self.rutas.limpio), self.fichas, self.cfg,
                            self.rutas.pronosticos, self.rutas.etiquetas)  # fmt: skip

    # --- entrenamiento --------------------------------------------------------------------

    def _sesion(self, reservar: bool = False) -> "Sesion":
        return Sesion(self, reservar)

    def etapa_ajustar(self) -> None:
        if self._hecho(self.carpeta_corrida / "mejor_prueba.json", "ajustar"):
            return
        s = self._sesion()
        from surco_planificador.entrenar import buscar

        mejor = buscar(s.cfg, s.dev, s.device, s.muestras["entrenamiento"], s.muestras["validacion"],
                       self.carpeta_corrida / "ajuste")  # fmt: skip
        (self.carpeta_corrida / "mejor_prueba.json").write_text(json.dumps(mejor, indent=1), encoding="utf-8")

    def etapa_entrenar(self) -> None:
        self._sesion().entrenar_final()

    def etapa_ablacion(self) -> None:
        self._sesion().ablacion()

    def etapa_evaluar(self) -> None:
        self._sesion().evaluar()

    def etapa_reserva(self) -> None:
        self._sesion(reservar=True).reserva()

    def etapa_exportar(self) -> None:
        fecha = datetime.now().astimezone().date().isoformat()
        self._sesion().exportar(self.rutas.modelos / f"planificador-{fecha}")


class Sesion:
    """Lo que comparten las etapas de entrenamiento: datos en el dispositivo, divisiones, config."""

    def __init__(self, tub: Tuberia, reservar: bool) -> None:
        from surco_planificador.datos_gpu import (
            a_dispositivo,
            ajustar_normalizacion,
            contextos,
            divisiones,
        )
        from surco_planificador.entrenar import cargar_config, elegir_dispositivo

        self.tub = tub
        self.cfg = cargar_config(tub.rutas.configs / "entrenamiento.yaml")
        self.datos = DatosLimpios.cargar(tub.rutas.limpio)
        self.etq = Etiquetas.cargar(tub.rutas.etiquetas)
        self.dis = diseno(tub.cfg)
        self.device = elegir_dispositivo()
        self.muestras = divisiones(self.datos, self.etq, self.dis, tub.cfg, reservar)
        anom = np.load(tub.rutas.etiquetas / "anomalias.npy", mmap_mode="r")
        self.ctx = contextos(self.datos, self.dis)
        self.norm = ajustar_normalizacion(self.datos, anom, self.ctx, self.muestras["entrenamiento"],
                                          self.dis, tub.fichas, tub.cfg)  # fmt: skip
        self.dev = a_dispositivo(self.datos, self.etq, anom, self.ctx, self.dis, tub.fichas, self.norm,
                                 self.device, self.cfg.ejecucion.pronostico_en)  # fmt: skip
        log.info("muestras: %s", {k: len(v) for k, v in self.muestras.items()})

    def _ajustado(self) -> tuple[Any, dict[str, Any]]:
        ruta = self.tub.carpeta_corrida / "mejor_prueba.json"
        if not ruta.exists():
            return self.cfg, {}
        mejor = json.loads(ruta.read_text(encoding="utf-8"))
        return replace(self.cfg, optim=replace(self.cfg.optim, **mejor["optim"])), mejor["modelo"]

    def _entrenador(self, nombre: str, semilla: int | None = None) -> Any:
        from surco_planificador.entrenar import Entrenador

        cfg, extra = self._ajustado()
        return Entrenador(cfg, self.dev, self.device, self.tub.carpeta_corrida / nombre, extra, semilla)

    def entrenar_final(self) -> None:
        """Parada temprana en validación; luego un reajuste en entrenamiento+validación por semilla."""
        r = self._entrenador("principal").ajustar(self.muestras["entrenamiento"], self.muestras["validacion"])
        log.info("principal: mejor época %d, validación %.5f", r.mejor_epoca, r.mejor_val)
        (self.tub.carpeta_corrida / "epocas.json").write_text(json.dumps({"epocas": max(r.mejor_epoca, 1)}))
        for semilla in self.cfg.ejecucion.semillas_conjunto:
            self._entrenador(f"reajuste_{semilla}", semilla).ajustar(
                self.muestras["entrenamiento+validacion"], None, epocas=max(r.mejor_epoca, 1))

    def ablacion(self) -> None:
        """La misma red sin el pronóstico: mide cuánto aporta (y equivale al modelo de FieldPilot)."""
        self.dev.usar_pronostico = False
        r = self._entrenador("sin_pronostico").ajustar(self.muestras["entrenamiento"], self.muestras["validacion"])
        log.info("sin pronóstico: mejor época %d, validación %.5f", r.mejor_epoca, r.mejor_val)
        self.dev.usar_pronostico = True

    def conjunto(self) -> Any:
        from surco_planificador.modelo import Conjunto

        return Conjunto([self._entrenador(f"reajuste_{s}", s).cargar_mejor()
                         for s in self.cfg.ejecucion.semillas_conjunto]).to(self.device)  # fmt: skip

    def _puntajes(self, conjunto: Any, clave: str = "prueba") -> tuple[dict[str, Any], dict[str, Any]]:
        from surco_planificador.entrenar import predecir
        from surco_planificador.evaluar import de_b0, de_b1, de_bf, de_red, por_enso, puntuar

        m = self.muestras[clave]
        prec = self.cfg.ejecucion.precision
        preds = {"red": de_red(predecir(conjunto, self.dev, m, self.device, prec), True),
                 "b0_climatologia": de_b0(self.etq, m), "bf_pronostico": de_bf(self.etq, m),
                 "b1_historia": de_b1(self.etq, self.dis, self.muestras["entrenamiento"], m)}  # fmt: skip
        sin = self.tub.carpeta_corrida / "sin_pronostico" / "mejor.pt"
        if sin.exists():
            self.dev.usar_pronostico = False
            red_sin = self._entrenador("sin_pronostico").cargar_mejor()
            preds["red_sin_pronostico"] = de_red(predecir(red_sin, self.dev, m, self.device, prec), False)
            self.dev.usar_pronostico = True
        puntajes = {n: puntuar(p, self.etq, m) for n, p in preds.items()}
        return puntajes, por_enso(preds, self.etq, m, self.ctx[m.s, 0])

    def evaluar(self) -> None:
        from surco_planificador.evaluar import adoptada

        puntajes, enso = self._puntajes(self.conjunto())
        p = self.tub.cfg.prueba
        self._escribir({"anios_prueba": f"{p.primero}-{p.ultimo}", "prueba": puntajes, "por_enso": enso,
                        "adoptado": adoptada(puntajes["red"], puntajes["b0_climatologia"],
                                             puntajes["bf_pronostico"])})  # fmt: skip
        log.info("métricas de prueba:\n%s", json.dumps(puntajes, indent=1))

    def reserva(self) -> None:
        """Entrena sin los departamentos reservados y prueba solo en ellos."""
        from surco_planificador.modelo import Conjunto

        e = self._entrenador("reserva")
        e.ajustar(self.muestras["entrenamiento"], self.muestras["validacion"])
        puntajes, _ = self._puntajes(Conjunto([e.cargar_mejor()]).to(self.device))
        self._escribir({"reserva": puntajes})
        log.info("métricas de la prueba espacial:\n%s", json.dumps(puntajes, indent=1))

    def _escribir(self, nuevo: dict[str, Any]) -> None:
        ruta = self.tub.carpeta_corrida / "metricas.json"
        actual = json.loads(ruta.read_text(encoding="utf-8")) if ruta.exists() else {}
        ruta.parent.mkdir(parents=True, exist_ok=True)
        ruta.write_text(json.dumps({**actual, **nuevo}, indent=1), encoding="utf-8")

    def exportar(self, carpeta: Path) -> None:
        import torch

        from surco_planificador.exportar import escribir_bundle

        metricas = json.loads((self.tub.carpeta_corrida / "metricas.json").read_text(encoding="utf-8"))
        m = self.muestras["prueba"].primeras(64)
        entradas, _, _ = self.dev.lote(torch.as_tensor(m.p, device=self.device), torch.as_tensor(m.s, device=self.device))
        datos = {"catalogo": self.tub.origen_catalogo, "huella_fichas": huella(self.tub.fichas),
                 "corrida": self.tub.corrida, "sistema_pronostico": f"{self.tub.cfg.pronostico.centro}-"
                 f"{self.tub.cfg.pronostico.sistema}"}  # fmt: skip
        man = escribir_bundle(carpeta, self.conjunto(), entradas, self.norm, self.tub.fichas, self.tub.fuera,
                              metricas, self.tub.rutas.etiquetas / "correccion.npz", datos)  # fmt: skip
        log.info("bundle en %s (paridad ONNX %.2e)", carpeta, man["paridad_onnx"])
