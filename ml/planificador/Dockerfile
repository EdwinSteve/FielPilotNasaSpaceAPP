# Servidor del planificador estacional (modelo ONNX en CPU) para el backend.
# Se construye desde la RAÍZ del repositorio, porque también necesita el catálogo de cultivos
# (data/catalogo_cultivos/datos/cultivos.json). Qué entra a la imagen lo decide /.dockerignore.
#   docker build -f ml/planificador/Dockerfile -t surco-planificador .
#   docker compose up --build surco-planificador      (ver compose.yaml en la raíz)
# En Render lo construye el servicio field-shift-planificador de render.yaml.

FROM python:3.12-slim
# rasterio (SoilGrids) trae GDAL en su wheel, pero enlaza contra libexpat del sistema, que la imagen slim
# no incluye: sin ella falla "ImportError: libexpat.so.1" en la primera consulta de suelo.
RUN apt-get update && apt-get install -y --no-install-recommends libexpat1 && rm -rf /var/lib/apt/lists/*
COPY --from=ghcr.io/astral-sh/uv:0.13.1 /uv /usr/local/bin/uv
ENV UV_COMPILE_BYTECODE=1 UV_LINK_MODE=copy UV_PYTHON_DOWNLOADS=never PYTHONUTF8=1 \
    SURCO_PLANIFICADOR_DATOS=/datos
WORKDIR /app/ml/planificador

# Dependencias primero (capa en caché): núcleo + "datos" (rasterio para SoilGrids), sin torch ni dev
COPY ml/planificador/pyproject.toml ml/planificador/uv.lock ./
RUN uv sync --locked --no-dev --extra datos --no-install-project

COPY ml/planificador/surco_planificador surco_planificador
COPY ml/planificador/configs configs
COPY ml/planificador/modelos modelos
COPY data/catalogo_cultivos/datos/cultivos.json /app/data/catalogo_cultivos/datos/cultivos.json
RUN uv sync --locked --no-dev --extra datos

# Sin privilegios; la caché de POWER/SoilGrids va en /datos (volumen en Docker Compose)
RUN useradd --uid 10002 --create-home planificador && mkdir -p /datos && chown planificador /datos
USER 10002
# Escucha en $PORT si la plataforma lo define (Render), si no en 8090
ENV PORT=8090
EXPOSE 8090
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s \
  CMD ["/app/ml/planificador/.venv/bin/python", "-c", "import os, urllib.request; urllib.request.urlopen(f\"http://localhost:{os.environ.get('PORT', '8090')}/salud\")"]
CMD ["sh", "-c", "exec /app/ml/planificador/.venv/bin/python -m surco_planificador.servidor --puerto \"$PORT\""]
