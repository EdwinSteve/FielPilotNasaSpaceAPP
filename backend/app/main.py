"""Punto de entrada: crea la app y monta el router de cada módulo bajo /api/v1."""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import get_settings
from app.core.errors import register_handler_errors
from app.modules.alertas.api.router import router as alertas
from app.modules.catalogo.api.router import router as catalogo
from app.modules.diagnostico.api.router import router as diagnostico
from app.modules.fincas.api.router import router as fincas
from app.modules.identidad.api.router import router as identidad
from app.modules.recomendacion.api.router import router as recomendacion
from app.modules.rotacion.api.router import router as rotacion
from app.modules.territorio.api.router import router as territorio

ROUTERS = [identidad, territorio, fincas, diagnostico, catalogo, recomendacion, rotacion, alertas]


def create_app() -> FastAPI:
    settings = get_settings()
    app = FastAPI(title="SURCO API", version="0.1.0")
    register_handler_errors(app)
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origenes_lista,
        allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE"],
        allow_headers=["Authorization", "Content-Type"],
    )
    for router in ROUTERS:
        app.include_router(router, prefix="/api/v1")

    @app.get("/api/v1/health", tags=["sistema"])
    async def health() -> dict[str, str]:
        return {"status": "ok"}

    return app


app = create_app()
