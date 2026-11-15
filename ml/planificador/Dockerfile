# Servidor del planificador estacional (modelo ONNX en CPU) para el backend.
#   docker compose up --build surco-planificador      (ver compose.yaml en la raíz)
# El catálogo de cultivos llega por el contexto adicional "catalogo" (data/catalogo_cultivos/datos).

FROM python:3.12-slim
COPY --from=ghcr.io/astral-sh/uv:0.13.1 /uv /usr/local/bin/uv
ENV UV_COMPILE_BYTECODE=1 UV_LINK_MODE=copy UV_PYTHON_DOWNLOADS=never PYTHONUTF8=1 \
    SURCO_PLANIFICADOR_DATOS=/datos
WORKDIR /app/ml/planificador

# Dependencias primero (capa en caché): núcleo + "datos" (rasterio para SoilGrids), sin torch ni dev
COPY pyproject.toml uv.lock ./
RUN uv sync --locked --no-dev --extra datos --no-install-project

COPY surco_planificador surco_planificador
COPY configs configs
COPY modelos modelos
COPY --from=catalogo cultivos.json /app/data/catalogo_cultivos/datos/cultivos.json
RUN uv sync --locked --no-dev --extra datos

# Sin privilegios; la caché de POWER/SoilGrids va en el volumen /datos
RUN useradd --uid 10002 --create-home planificador && mkdir -p /datos && chown planificador /datos
USER 10002
EXPOSE 8090
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s \
  CMD ["/app/ml/planificador/.venv/bin/python", "-c", "import urllib.request; urllib.request.urlopen('http://localhost:8090/salud')"]
CMD ["/app/ml/planificador/.venv/bin/python", "-m", "surco_planificador.servidor", "--puerto", "8090"]
