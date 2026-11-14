"""Error de dominio común y su traducción a HTTP: siempre {"code", "message", "details"?}."""
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse


class ErrorDominio(Exception):
    def __init__(self, code: str, message: str, http_status: int = 400):
        super().__init__(message)
        self.code = code
        self.message = message
        self.http_status = http_status


def register_handler_errors(app: FastAPI) -> None:
    @app.exception_handler(ErrorDominio)
    async def _error_dominio(_: Request, exc: ErrorDominio) -> JSONResponse:
        return JSONResponse(
            status_code=exc.http_status,
            content={"code": exc.code, "message": exc.message},
        )
