"""Servidor HTTP del planificador: valida la solicitud, responde JSON y guarda el plan del día en caché."""

import json
import threading
import urllib.error
import urllib.request
from http.server import ThreadingHTTPServer
from pathlib import Path
from typing import Any

import pytest

from surco_planificador.servidor import ErrorSolicitud, Planificacion, crear_manejador, leer_solicitud


class MotorFalso:
    bundle = Path("planificador-prueba")

    def __init__(self) -> None:
        self.llamadas: list[tuple[float, float, bool]] = []

    def planificar(self, lat: float, lon: float, riego: bool) -> dict[str, Any]:
        self.llamadas.append((lat, lon, riego))
        if lat > 13:
            raise ValueError("SoilGrids no tiene un perfil completo")
        return {"lote": {"lat": lat, "lon": lon}, "mejor": None, "opciones": []}


def test_leer_solicitud() -> None:
    assert leer_solicitud(b'{"lat": 5.07, "lon": -75.52, "riego": true}') == (5.07, -75.52, True)
    assert leer_solicitud(b'{"lat": "5", "lon": "-75"}') == (5.0, -75.0, False)
    with pytest.raises(ErrorSolicitud, match="lat y lon"):
        leer_solicitud(b'{"lat": 5}')
    with pytest.raises(ErrorSolicitud, match="Colombia"):
        leer_solicitud(b'{"lat": 40.4, "lon": -3.7}')


@pytest.fixture
def servidor() -> Any:
    motor = MotorFalso()
    httpd = ThreadingHTTPServer(("127.0.0.1", 0), crear_manejador(Planificacion(motor)))  # type: ignore[arg-type]
    hilo = threading.Thread(target=httpd.serve_forever, daemon=True)
    hilo.start()
    yield f"http://127.0.0.1:{httpd.server_address[1]}", motor
    httpd.shutdown()


def _post(url: str, cuerpo: dict[str, Any]) -> tuple[int, dict[str, Any]]:
    req = urllib.request.Request(url, json.dumps(cuerpo).encode(), {"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, json.loads(r.read())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read())


def test_planifica_y_guarda_el_plan_del_dia(servidor: Any) -> None:
    base, motor = servidor
    with urllib.request.urlopen(f"{base}/salud") as r:
        assert json.loads(r.read()) == {"estado": "ok", "modelo": "planificador-prueba"}
    estado, plan = _post(f"{base}/planificar", {"lat": 5.07, "lon": -75.52})
    assert estado == 200 and plan["lote"] == {"lat": 5.07, "lon": -75.52}
    _post(f"{base}/planificar", {"lat": 5.07, "lon": -75.52})
    assert motor.llamadas == [(5.07, -75.52, False)]  # la segunda vez, de la caché


def test_errores_con_codigo(servidor: Any) -> None:
    base, _ = servidor
    estado, cuerpo = _post(f"{base}/planificar", {"lat": 40.4, "lon": -3.7})
    assert estado == 400 and cuerpo["code"] == "PLANIFICADOR_FUERA_DE_COLOMBIA"
    estado, cuerpo = _post(f"{base}/planificar", {"lat": 13.5, "lon": -81.7})
    assert estado == 400 and cuerpo["code"] == "PLANIFICADOR_TERRENO_SIN_DATOS"
    estado, _ = _post(f"{base}/otra", {})
    assert estado == 404
