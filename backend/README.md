# Backend SURCO (FastAPI)

Monolito modular: cada módulo de `app/modules/` tiene sus capas `domain`, `application`, `infrastructure` y `api`, y su propio esquema de PostgreSQL. Reglas en [docs/arquitectura.md](../docs/arquitectura.md).

| Módulo | Qué hace | Almacenamiento |
|---|---|---|
| `identidad` | Registro, login, refresh y logout con JWT; política de datos | PostgreSQL `identidad` |
| `territorio` | Municipios y centros poblados (DIVIPOLA) y elevación SRTM | JSON en memoria |
| `fincas` | Fincas, lotes y tipo de riego | PostgreSQL `fincas` |
| `diagnostico` | Resumen climatológico del lote y su suelo | PostgreSQL `diagnostico` |
| `catalogo` | Requerimientos de los 44 cultivos por fase | MongoDB `surco_agro` |
| `recomendacion` | Cultivos aptos según el modelo predictivo | PostgreSQL `recomendacion` |
| `rotacion` | Ciclos productivos con mejor resultado bajo el clima esperado, efecto en el suelo y escenarios | PostgreSQL `rotacion` |
| `alertas` | Pronósticos del IDEAM de lluvia y sequía, alertas y notificaciones; expone el pronóstico del lote a `rotacion` | PostgreSQL `alertas` |

## Correr

```bash
python3 -m venv .venv && source .venv/bin/activate
pip install -e ".[dev]"
pip install -e "../ml[gee,modelo]"
cp .env.example .env
alembic upgrade head
uvicorn app.main:app --reload --port 8000     # http://localhost:8000/docs
```

## Tareas periódicas

`python scripts/revisar_alertas_ideam.py` consulta el IDEAM y notifica las alertas nuevas. En producción corre con cron cada 6 h ([ADR 0006](../docs/decisiones/0006-alertas-ideam.md)).

## Pruebas

| Carpeta | Qué prueba | Necesita |
|---|---|---|
| `tests/unit/<modulo>/` | Dominio y casos de uso con *fakes* de los puertos | Nada |
| `tests/api/<modulo>/` | Rutas HTTP con los casos de uso simulados | Nada |
| `tests/integration/` | Repositorios contra PostgreSQL | `TEST_DATABASE_URL` hacia una base `*_test` |
| `tests/test_arquitectura.py` | Que el dominio sea puro y que los módulos no se crucen | Nada |

Los errores siempre llegan como `{ "code", "message", "details"? }`. Un recurso de otro productor responde 404, igual que uno inexistente.
