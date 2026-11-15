#!/usr/bin/env bash
# Entrena el planificador estacional en la máquina con la RTX 5060 Ti (Linux).
#
#   ./entrenar.sh                          # todas las etapas; cada una se salta si ya terminó
#   ./entrenar.sh entrenar evaluar         # solo algunas
#   ./entrenar.sh entrenar --forzar entrenar
#
# Antes, una sola vez: cuenta en el Climate Data Store, ~/.cdsapirc y la licencia del dataset
# aceptada (README.md §8.1). Opcional: SURCO_MONGO_URI para leer el catálogo desde MongoDB.
set -euo pipefail
cd "$(dirname "$0")"
export PYTHONUTF8=1

paso() { printf '\n\033[36m==> %s\033[0m\n' "$1"; }

paso "Driver NVIDIA"
if command -v nvidia-smi >/dev/null; then
  nvidia-smi --query-gpu=name,driver_version,memory.total --format=csv
else
  echo "AVISO: no está nvidia-smi; instale el driver 570 o más nuevo (serie RTX 50, Blackwell)." >&2
fi

paso "uv"
if ! command -v uv >/dev/null; then
  curl -LsSf https://astral.sh/uv/install.sh | sh
  export PATH="$HOME/.local/bin:$PATH"
fi
uv --version

paso "Entorno: PyTorch CUDA 12.8, ONNX, cdsapi, xarray, rasterio (la primera vez descarga varios GB)"
uv sync --extra datos --extra entrenamiento --extra mongo

if [ ! -f "$HOME/.cdsapirc" ] && [ -z "${CDSAPI_KEY:-}" ]; then
  echo "AVISO: falta ~/.cdsapirc: la etapa 'pronosticos' no podrá descargar SEAS5 (README.md §8.1)." >&2
fi

paso "Pruebas (incluye la tubería completa con datos sintéticos)"
uv run pytest -q

paso "Tubería: ${*:-todo}"
if command -v systemd-inhibit >/dev/null; then
  exec systemd-inhibit --what=idle:sleep --why="SURCO entrenando" uv run python -m surco_planificador "$@"
fi
exec uv run python -m surco_planificador "$@"
