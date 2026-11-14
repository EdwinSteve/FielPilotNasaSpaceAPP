"""Configuración leída del entorno (.env) con pydantic-settings."""
from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    database_url: str = "postgresql+asyncpg://surco:surco@localhost:5432/surco"
    mongo_url: str = "mongodb://localhost:27017/surco_agro"
    jwt_secreto: str = "cambiar-en-.env"
    gee_project: str | None = None
    datos_gov_app_token: str | None = None   # API de datos.gov.co (IDEAM)
    cors_origenes: str = "http://localhost:3000"

    @property
    def cors_origenes_lista(self) -> list[str]:
        return [o.strip() for o in self.cors_origenes.split(",") if o.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
