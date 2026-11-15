"""Servidor HTTP del planificador para el backend (módulo recomendacion). Solo biblioteca estándar.

    GET  /salud       -> {"estado": "ok", "modelo": "planificador-2026-11-15"}
    POST /planificar  {"lat": 5.07, "lon": -75.52, "riego": false}  -> Motor.planificar(...)

Carga el modelo una vez al arrancar. La primera consulta de una zona tarda (baja POWER desde 1981 y
SoilGrids); después queda en la caché de disco (data/planificador/crudo/servicio) y el plan del día
en memoria. Errores: 400 con {"code", "message"} si la solicitud o el terreno no sirven, 500 si falla
el modelo. Uso: python -m surco_planificador.servidor --puerto 8090
"""

import argparse
import json
import logging
import threading
from datetime import date
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any

from surco_planificador.servicio import Motor

log = logging.getLogger(__name__)
MAX_CUERPO = 4096


class Planificacion:
    """El motor más una caché del plan por terreno y día (los datos cambian a lo sumo una vez al día)."""

    def __init__(self, motor: Motor) -> None:
        self.motor = motor
        self._candado = threading.Lock()  # una consulta a la vez: la caché HTTP y SoilGrids escriben a disco
        self._planes: dict[tuple[float, float, bool, date], dict[str, Any]] = {}

    def planificar(self, lat: float, lon: float, riego: bool) -> dict[str, Any]:
        clave = (round(lat, 4), round(lon, 4), riego, date.today())
        with self._candado:
            if clave not in self._planes:
                if len(self._planes) > 500:
                    self._planes.clear()
                self._planes[clave] = self.motor.planificar(lat, lon, riego)
            return self._planes[clave]


class ErrorSolicitud(Exception):
    def __init__(self, codigo: str, mensaje: str) -> None:
        super().__init__(mensaje)
        self.codigo = codigo


def leer_solicitud(cuerpo: bytes) -> tuple[float, float, bool]:
    try:
        datos = json.loads(cuerpo or b"{}")
        lat, lon = float(datos["lat"]), float(datos["lon"])
    except (ValueError, KeyError, TypeError) as e:
        raise ErrorSolicitud("PLANIFICADOR_SOLICITUD_INVALIDA", "Envíe lat y lon en grados decimales.") from e
    if not (-4.3 <= lat <= 13.6 and -82.0 <= lon <= -66.8):
        raise ErrorSolicitud("PLANIFICADOR_FUERA_DE_COLOMBIA", "El terreno debe estar en Colombia.")
    return lat, lon, bool(datos.get("riego", False))


def crear_manejador(planificacion: Planificacion) -> type[BaseHTTPRequestHandler]:
    class Manejador(BaseHTTPRequestHandler):
        def _responder(self, estado: int, cuerpo: dict[str, Any]) -> None:
            datos = json.dumps(cuerpo, ensure_ascii=False).encode("utf-8")
            self.send_response(estado)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(datos)))
            self.end_headers()
            self.wfile.write(datos)

        def do_GET(self) -> None:
            if self.path == "/salud":
                self._responder(200, {"estado": "ok", "modelo": planificacion.motor.bundle.name})
            else:
                self._responder(404, {"code": "NO_ENCONTRADO", "message": "Ruta inexistente."})

        def do_POST(self) -> None:
            if self.path != "/planificar":
                self._responder(404, {"code": "NO_ENCONTRADO", "message": "Ruta inexistente."})
                return
            largo = int(self.headers.get("Content-Length") or 0)
            if largo > MAX_CUERPO:
                self._responder(413, {"code": "PLANIFICADOR_SOLICITUD_INVALIDA", "message": "Solicitud muy grande."})
                return
            try:
                lat, lon, riego = leer_solicitud(self.rfile.read(largo))
                self._responder(200, planificacion.planificar(lat, lon, riego))
            except ErrorSolicitud as e:
                self._responder(400, {"code": e.codigo, "message": str(e)})
            except ValueError as e:  # p. ej. SoilGrids sin perfil cerca del punto
                log.warning("terreno no planificable: %s", e)
                self._responder(400, {"code": "PLANIFICADOR_TERRENO_SIN_DATOS", "message": str(e)})
            except Exception:
                log.exception("falló la planificación")
                self._responder(500, {"code": "PLANIFICADOR_ERROR", "message": "No pudimos calcular la recomendación."})

        def log_message(self, formato: str, *args: Any) -> None:
            log.info("%s %s", self.address_string(), formato % args)

    return Manejador


def main() -> None:
    ap = argparse.ArgumentParser(description="Servidor HTTP del planificador estacional")
    ap.add_argument("--host", default="0.0.0.0")
    ap.add_argument("--puerto", type=int, default=8090)
    args = ap.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
    planificacion = Planificacion(Motor())
    log.info("modelo %s listo en http://%s:%d", planificacion.motor.bundle.name, args.host, args.puerto)
    ThreadingHTTPServer((args.host, args.puerto), crear_manejador(planificacion)).serve_forever()


if __name__ == "__main__":
    main()
