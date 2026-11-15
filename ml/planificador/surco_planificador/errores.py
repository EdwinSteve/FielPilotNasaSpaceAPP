"""Errores del servicio con código estable: servidor.py los traduce a HTTP y el backend a errores de dominio.

- TerrenoSinSuelo (422): SoilGrids no tiene un perfil completo cerca del punto.
- MesNoEntrenado (422): el bundle no se entrenó con pronósticos iniciados en ese mes.
- FuenteCaida (502): una fuente externa no respondió o respondió algo ilegible; `fuente` dice cuál.
"""


class ErrorPlanificador(Exception):
    codigo = "PLANIFICADOR_ERROR"
    estado_http = 500

    def __init__(self, mensaje: str) -> None:
        super().__init__(mensaje)


class TerrenoSinSuelo(ErrorPlanificador, ValueError):
    codigo = "PLANIFICADOR_TERRENO_SIN_SUELO"
    estado_http = 422


class MesNoEntrenado(ErrorPlanificador, ValueError):
    codigo = "PLANIFICADOR_MES_NO_ENTRENADO"
    estado_http = 422


class FuenteCaida(ErrorPlanificador):
    codigo = "PLANIFICADOR_FUENTE_CAIDA"
    estado_http = 502

    def __init__(self, fuente: str, detalle: str) -> None:
        super().__init__(f"{fuente} no respondió: {detalle}")
        self.fuente = fuente
