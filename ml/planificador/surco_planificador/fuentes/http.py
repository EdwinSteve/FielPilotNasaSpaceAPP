"""GET con reintentos y caché en disco por SHA-256 de la URL: una descarga cortada se retoma."""

import hashlib
import json
import time
import urllib.error
import urllib.parse
import urllib.request
from collections.abc import Mapping, Sequence
from pathlib import Path
from typing import Any

AGENTE = "SURCO-planificador/0.1 (investigacion; ver repositorio)"
REINTENTABLES = {429, 500, 502, 503, 504}


def url_con(base: str, consulta: Mapping[str, str | int | float | Sequence[str]]) -> str:
    return f"{base}?{urllib.parse.urlencode(consulta, doseq=True)}"


class HttpCache:
    def __init__(self, carpeta: Path, reintentos: int = 6, espera_s: float = 120.0) -> None:
        self._carpeta = carpeta
        self._reintentos = reintentos
        self._espera = espera_s
        carpeta.mkdir(parents=True, exist_ok=True)

    def ruta(self, url: str) -> Path:
        return self._carpeta / f"{hashlib.sha256(url.encode()).hexdigest()}.body"

    def texto(self, url: str) -> str:
        ruta = self.ruta(url)
        if ruta.exists():
            return ruta.read_text(encoding="utf-8")
        cuerpo = self._traer(url)
        tmp = ruta.with_suffix(".part")
        tmp.write_text(cuerpo, encoding="utf-8")
        tmp.replace(ruta)  # atómico: un corte nunca deja una entrada a medias
        return cuerpo

    def json(self, url: str) -> Any:
        return json.loads(self.texto(url))

    def _traer(self, url: str) -> str:
        pausa = 5.0
        for intento in range(1, self._reintentos + 1):
            try:
                pedido = urllib.request.Request(url, headers={"User-Agent": AGENTE})
                with urllib.request.urlopen(pedido, timeout=self._espera) as r:
                    return str(r.read().decode("utf-8"))
            except urllib.error.HTTPError as e:
                if e.code not in REINTENTABLES or intento == self._reintentos:
                    raise
            except (urllib.error.URLError, TimeoutError, ConnectionError):
                if intento == self._reintentos:
                    raise
            time.sleep(pausa)
            pausa = min(pausa * 2, 120.0)
        raise RuntimeError("inalcanzable")
