"""Servidor HTTP del planificador: valida la solicitud, responde JSON y guarda el plan del día en caché."""

import json
import threading
import urllib.error
import urllib.request
from datetime import date, timedelta
from http.server import ThreadingHTTPServer
from pathlib import Path
from typing import Any

import pytest

from surco_planificador.errores import FuenteCaida, MesNoEntrenado, TerrenoSinSuelo
from surco_planificador.servidor import ErrorSolicitud, Planificacion, crear_manejador, leer_solicitud


class MotorFalso:
    """Sin red ni modelo: responde como Motor.planificar y simula las fallas de las fuentes."""

    bundle = Path("planificador-prueba")
    motor = "bf"

    def __init__(self) -> None:
        self.llamadas: list[tuple[float, float, bool, list[date] | None]] = []

    def planificar(self, lat: float, lon: float, riego: bool, hoy: date | None = None,
                   siembras: list[date] | None = None) -> dict[str, Any]:
        self.llamadas.append((lat, lon, riego, siembras))
        if lat > 13:
            raise TerrenoSinSuelo("SoilGrids no tiene un perfil de suelo completo")
        if lat < -4:
            raise FuenteCaida("NASA POWER", "HTTP Error 503")
        if lon > -67:
            raise MesNoEntrenado("el modelo no se entrenó con pronósticos iniciados en el mes 7")
        return {"lote": {"lat": lat, "lon": lon}, "mejor": None, "opciones": [], "motor": {"id": "bf"}}


def test_leer_solicitud() -> None:
    assert leer_solicitud(b'{"lat": 5.07, "lon": -75.52, "riego": true}') == (5.07, -75.52, True, None)
    assert leer_solicitud(b'{"lat": "5", "lon": "-75"}') == (5.0, -75.0, False, None)
    manana = date.today() + timedelta(days=1)
    assert leer_solicitud(json.dumps({"lat": 5, "lon": -75, "siembra": manana.isoformat()}).encode())[3] == manana
    with pytest.raises(ErrorSolicitud, match="lat y lon"):
        leer_solicitud(b'{"lat": 5}')
    with pytest.raises(ErrorSolicitud, match="Colombia"):
        leer_solicitud(b'{"lat": 40.4, "lon": -3.7}')
    with pytest.raises(ErrorSolicitud, match="AAAA-MM-DD"):
        leer_solicitud(b'{"lat": 5, "lon": -75, "siembra": "20/11/2026"}')
    with pytest.raises(ErrorSolicitud, match="60"):
        leer_solicitud(json.dumps({"lat": 5, "lon": -75, "siembra": "2020-01-01"}).encode())


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
        assert json.loads(r.read()) == {"estado": "ok", "modelo": "planificador-prueba", "motor": "bf"}
    estado, plan = _post(f"{base}/planificar", {"lat": 5.07, "lon": -75.52})
    assert estado == 200 and plan["lote"] == {"lat": 5.07, "lon": -75.52}
    _post(f"{base}/planificar", {"lat": 5.07, "lon": -75.52})
    assert motor.llamadas == [(5.07, -75.52, False, None)]  # la segunda vez, de la caché


def test_errores_con_codigo(servidor: Any) -> None:
    base, _ = servidor
    estado, cuerpo = _post(f"{base}/planificar", {"lat": 40.4, "lon": -3.7})
    assert estado == 400 and cuerpo["code"] == "PLANIFICADOR_FUERA_DE_COLOMBIA"
    estado, cuerpo = _post(f"{base}/planificar", {"lat": 13.5, "lon": -81.7})
    assert estado == 422 and cuerpo["code"] == "PLANIFICADOR_TERRENO_SIN_SUELO"
    estado, cuerpo = _post(f"{base}/planificar", {"lat": 5.0, "lon": -66.9})
    assert estado == 422 and cuerpo["code"] == "PLANIFICADOR_MES_NO_ENTRENADO"
    estado, cuerpo = _post(f"{base}/planificar", {"lat": -4.2, "lon": -70.0})
    assert estado == 502 and cuerpo["code"] == "PLANIFICADOR_FUENTE_CAIDA" and cuerpo["fuente"] == "NASA POWER"
    estado, _ = _post(f"{base}/otra", {})
    assert estado == 404


def test_siembra_fija_pide_una_sola_fecha(servidor: Any) -> None:
    base, motor = servidor
    manana = date.today() + timedelta(days=1)
    estado, _ = _post(f"{base}/planificar", {"lat": 4.6, "lon": -74.1, "siembra": manana.isoformat()})
    assert estado == 200 and motor.llamadas[-1][3] == [manana]


def test_la_cache_en_disco_sobrevive_un_reinicio(tmp_path: Path) -> None:
    primero = MotorFalso()
    Planificacion(primero, tmp_path).planificar(5.07, -75.52, False)  # type: ignore[arg-type]
    segundo = MotorFalso()
    plan = Planificacion(segundo, tmp_path).planificar(5.07, -75.52, False)  # type: ignore[arg-type]
    assert plan["lote"] == {"lat": 5.07, "lon": -75.52}
    assert segundo.llamadas == []  # salió del disco
    Planificacion(segundo, tmp_path).planificar(5.07, -75.52, True)  # type: ignore[arg-type]
    assert len(segundo.llamadas) == 1  # con riego es otra consulta


def test_acepta_cuerpo_chunked_como_lo_manda_restclient(servidor: Any) -> None:
    """RestClient de Spring manda el JSON en bloques si no conoce la longitud: antes llegaba vacío."""
    import http.client

    base, motor = servidor
    host, puerto = base.removeprefix("http://").split(":")
    con = http.client.HTTPConnection(host, int(puerto), timeout=10)
    cuerpo = json.dumps({"lat": 2.9275, "lon": -75.2875, "riego": True}).encode()
    con.request("POST", "/planificar", body=iter([cuerpo[:10], cuerpo[10:]]),
                headers={"Content-Type": "application/json"}, encode_chunked=True)
    r = con.getresponse()
    assert r.status == 200, r.read()
    assert motor.llamadas[-1][:3] == (2.9275, -75.2875, True)
    con.close()


def test_un_cuerpo_chunked_enorme_es_413(servidor: Any) -> None:
    import http.client

    base, _ = servidor
    host, puerto = base.removeprefix("http://").split(":")
    con = http.client.HTTPConnection(host, int(puerto), timeout=10)
    con.request("POST", "/planificar", body=iter([b"x" * 3000, b"y" * 3000]), encode_chunked=True)
    assert con.getresponse().status == 413
    con.close()
