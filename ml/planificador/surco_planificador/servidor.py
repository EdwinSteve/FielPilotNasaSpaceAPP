"""Servidor HTTP del planificador para el backend (módulo recomendacion). Solo biblioteca estándar.

    GET  /salud       -> {"estado": "ok", "modelo": "planificador-2026-11-15", "motor": "bf"}
    POST /planificar  {"lat": 5.07, "lon": -75.52, "riego": false, "siembra": "2026-12-01"?}
                      -> Motor.planificar(...). Sin "siembra" compara 6 fechas del próximo mes.

Carga el modelo una vez al arrancar. Latencias medidas: la primera consulta de una zona baja NASA POWER
desde 1981, la elevación y SoilGrids (1-3 minutos según la red); con esos datos en la caché de disco
(data/planificador/crudo/servicio) una consulta nueva tarda unos 3 s, y la misma consulta del mismo día
sale de la caché de respuestas (memoria y disco, sobrevive a reinicios) en milisegundos.

Errores, siempre {"code", "message"} (y "fuente" en el 502):
- 400 PLANIFICADOR_SOLICITUD_INVALIDA / PLANIFICADOR_FUERA_DE_COLOMBIA: la solicitud no sirve.
- 422 PLANIFICADOR_TERRENO_SIN_SUELO: SoilGrids no tiene perfil de suelo cerca del punto.
- 422 PLANIFICADOR_MES_NO_ENTRENADO: el modelo no se entrenó con pronósticos de ese mes.
- 502 PLANIFICADOR_FUENTE_CAIDA: NASA POWER, SoilGrids, Open-Meteo o NOAA no respondieron.
- 500 PLANIFICADOR_ERROR: falla interna.
Uso: python -m surco_planificador.servidor --puerto 8090
"""

import argparse
import hashlib
import json
import logging
import threading
from datetime import date, timedelta
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any

from surco_planificador.errores import ErrorPlanificador, FuenteCaida
from surco_planificador.servicio import Motor

log = logging.getLogger(__name__)
MAX_CUERPO = 4096


Solicitud = tuple[float, float, bool, date | None]


class Planificacion:
    """El motor más una caché de respuestas por terreno, riego, siembra y día (los datos cambian a lo sumo
    una vez al día). En memoria y, si hay `carpeta`, en disco para que un reinicio no repita el cálculo."""

    def __init__(self, motor: Motor, carpeta: Path | None = None) -> None:
        self.motor = motor
        self.carpeta = carpeta
        self._candado = threading.Lock()  # una consulta a la vez: la caché HTTP y SoilGrids escriben a disco
        self._planes: dict[str, dict[str, Any]] = {}
        if carpeta:
            carpeta.mkdir(parents=True, exist_ok=True)

    def _clave(self, lat: float, lon: float, riego: bool, siembra: date | None) -> str:
        motor = getattr(self.motor, "motor", "bf")
        texto = f"{lat:.4f}|{lon:.4f}|{riego}|{siembra}|{date.today()}|{motor}|{self.motor.bundle.name}"
        return hashlib.sha256(texto.encode()).hexdigest()[:32]

    def planificar(self, lat: float, lon: float, riego: bool, siembra: date | None = None) -> dict[str, Any]:
        clave = self._clave(lat, lon, riego, siembra)
        with self._candado:
            if clave in self._planes:
                return self._planes[clave]
            ruta = self.carpeta / f"{clave}.json" if self.carpeta else None
            if ruta and ruta.exists():
                plan = json.loads(ruta.read_text(encoding="utf-8"))
            else:
                plan = (self.motor.planificar(lat, lon, riego, hoy=siembra - timedelta(days=1), siembras=[siembra])
                        if siembra else self.motor.planificar(lat, lon, riego))  # fmt: skip
                if ruta:
                    tmp = ruta.with_suffix(".part")
                    tmp.write_text(json.dumps(plan, ensure_ascii=False), encoding="utf-8")
                    tmp.replace(ruta)
            if len(self._planes) > 500:
                self._planes.clear()
            self._planes[clave] = plan
            return plan


class ErrorSolicitud(Exception):
    def __init__(self, codigo: str, mensaje: str) -> None:
        super().__init__(mensaje)
        self.codigo = codigo


def leer_solicitud(cuerpo: bytes) -> Solicitud:
    try:
        datos = json.loads(cuerpo or b"{}")
        lat, lon = float(datos["lat"]), float(datos["lon"])
    except (ValueError, KeyError, TypeError) as e:
        raise ErrorSolicitud("PLANIFICADOR_SOLICITUD_INVALIDA", "Envíe lat y lon en grados decimales.") from e
    if not (-4.3 <= lat <= 13.6 and -82.0 <= lon <= -66.8):
        raise ErrorSolicitud("PLANIFICADOR_FUERA_DE_COLOMBIA", "El terreno debe estar en Colombia.")
    siembra = None
    if datos.get("siembra"):
        try:
            siembra = date.fromisoformat(str(datos["siembra"]))
        except ValueError as e:
            raise ErrorSolicitud("PLANIFICADOR_SOLICITUD_INVALIDA", "La siembra va como AAAA-MM-DD.") from e
        if not date.today() <= siembra <= date.today() + timedelta(days=60):
            raise ErrorSolicitud("PLANIFICADOR_SOLICITUD_INVALIDA",
                                 "La siembra debe estar entre hoy y los próximos 60 días.")  # fmt: skip
    return lat, lon, bool(datos.get("riego", False)), siembra


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
                self._responder(200, {"estado": "ok", "modelo": planificacion.motor.bundle.name,
                                      "motor": getattr(planificacion.motor, "motor", "bf")})  # fmt: skip
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
                lat, lon, riego, siembra = leer_solicitud(self.rfile.read(largo))
                self._responder(200, planificacion.planificar(lat, lon, riego, siembra))
            except ErrorSolicitud as e:
                self._responder(400, {"code": e.codigo, "message": str(e)})
            except FuenteCaida as e:
                log.warning("fuente caída: %s", e)
                self._responder(e.estado_http, {"code": e.codigo, "message": str(e), "fuente": e.fuente})
            except ErrorPlanificador as e:  # terreno sin suelo, mes no entrenado
                log.warning("terreno no planificable: %s", e)
                self._responder(e.estado_http, {"code": e.codigo, "message": str(e)})
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
    motor = Motor()
    planificacion = Planificacion(motor, motor.rutas.crudo / "servicio" / "planes")
    log.info("modelo %s (motor %s) listo en http://%s:%d", motor.bundle.name, motor.motor, args.host, args.puerto)
    ThreadingHTTPServer((args.host, args.puerto), crear_manejador(planificacion)).serve_forever()


if __name__ == "__main__":
    main()
