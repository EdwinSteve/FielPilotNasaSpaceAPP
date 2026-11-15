# Planificador estacional: el mejor ciclo productivo con el clima de los próximos meses

Red neuronal que, para un lote y una fecha de siembra, puntúa los 23 cultivos de ciclo estacional del
catálogo `surco_agro` con la historia del lote (NASA POWER), el **pronóstico estacional ECMWF SEAS5**
(Copernicus C3S), el suelo (SoilGrids) y el ENSO. Se entrena en una RTX 5060 Ti y se sirve en
**cualquier CPU** con ONNX Runtime.

La investigación, las fuentes y por qué se eligió esta arquitectura están en
**[INVESTIGACION.md](INVESTIGACION.md)**. Este README cuenta cómo correrlo.

## 1. Qué responde

Para cada cultivo, sembrado hoy en este lote:

| Salida | Significado |
|---|---|
| `rend_p10/p50/p90` | Rendimiento relativo en secano (0–1) |
| `prob_exito` | P(rendimiento relativo ≥ 0,6) |
| `riego_p10/p50/p90` | Riego necesario para no sufrir estrés hídrico (mm) |
| `fraccion_calor`, `fraccion_frio` | Fracción del ciclo fuera del óptimo térmico de cada fase |
| `rend_riego_p10/p50/p90` | Rendimiento relativo si el lote tiene riego |

El backend ordena por `rend_p50`, o por `rend_riego_p50` si el lote tiene riego.

## 2. Etapas

```
entorno → descargar → pronosticos → limpiar → interpolar → etiquetas → ajustar → entrenar
        → ablacion → evaluar → reserva → exportar
```

| Etapa | Qué hace | Tiempo estimado |
|---|---|---|
| `entorno` | Revisa CUDA, sm_120 y bf16 | segundos |
| `puntos` | 202 puntos (uno por celda POWER) desde DIVIPOLA → `configs/puntos.csv` (versionado) | 1 min |
| `descargar` | POWER, SoilGrids (54 capas) y ONI | 10–20 min |
| `pronosticos` | 528 inicios de SEAS5 desde el CDS (1982–2025), ~23 GB | **horas a 2 días** (cola del CDS) |
| `limpiar` | Calidad, ET0 FAO-56, climatología sin fuga | 5–10 min |
| `interpolar` | NetCDF → memmap por punto (~7 GB) | 20–40 min |
| `etiquetas` | Corrección de sesgo, oráculo (verdad, B0, Bf por miembro), anomalías | 30–60 min (CPU, 4 procesos) |
| `ajustar` | 16 pruebas × 12 épocas | ~1 h (GPU) |
| `entrenar` | Corrida principal + 5 reajustes (conjunto) | ~1 h |
| `ablacion` | La misma red **sin** pronóstico: mide cuánto aporta | ~10 min |
| `evaluar` | Prueba 2017–2025 frente a B0, Bf, B1 y la ablación, también por fase ENSO | minutos |
| `reserva` | Entrena sin Huila y prueba solo en Huila | ~10 min |
| `exportar` | `model.onnx` del conjunto + paridad + prueba INT8 → `modelos/planificador-<fecha>/` | 1 min |

Cada etapa se salta si su salida existe, y el entrenamiento se reanuda desde `ultimo.pt`. Tras un
corte, se vuelve a correr el mismo comando. Para rehacer una: `--forzar entrenar`.

## 3. Correrlo en la máquina de la RTX 5060 Ti

### 3.0 En Windows: tres pasos

1. **Una sola vez, en el navegador:** acepte la licencia del dataset en
   https://cds.climate.copernicus.eu/datasets/seasonal-original-single-levels?tab=download
   (al final de la página, con la cuenta dueña de la clave). Sin esto el CDS responde 403.
2. Traiga el código: `git fetch` y `git checkout feat/planificador-estacional` (o clone el repositorio).
3. Doble clic en **`ml\planificador\entrenar_windows.bat`**. La primera vez pide la clave del CDS y crea
   `%USERPROFILE%\.cdsapirc`. Luego corre todo solo: entorno, pruebas, descargas, etiquetas, ajuste,
   entrenamiento, evaluación y exportación.

Si algo se interrumpe (red, luz, cola del CDS), vuelva a hacer doble clic: sigue donde iba. Solo
necesita el driver NVIDIA ≥ 570, ~60 GB libres y conexión a internet; Python, PyTorch y lo demás
los instala el script.

### 3.1 Credenciales del Climate Data Store (si prefiere crearlas a mano)

1. Cree una cuenta en https://cds.climate.copernicus.eu y copie su *Personal Access Token*.
2. Cree `~/.cdsapirc` (en Windows, `%USERPROFILE%\.cdsapirc`):
   ```
   url: https://cds.climate.copernicus.eu/api
   key: <su-token>
   ```
3. En la página del dataset `seasonal-original-single-levels`, acepte la licencia (pestaña
   *Download*, al final).

Opcional: `SURCO_MONGO_URI=mongodb://...` para leer el catálogo desde MongoDB (base `surco_agro`, o
`SURCO_MONGO_DB`). Sin ella se lee `data/catalogo_cultivos/datos/cultivos.json`.
`SURCO_PLANIFICADOR_DATOS=D:\surco-datos` mueve los ~40 GB de datos a otro disco.

### 3.2 Entrenar

```bash
# Linux
ml/planificador/entrenar.sh

# Windows
powershell -NoProfile -ExecutionPolicy Bypass -File ml\planificador\entrenar_windows.ps1
```

El script instala uv, crea el entorno desde `uv.lock` (torch 2.11 + CUDA 12.8; necesita el driver
≥ 570), corre las pruebas y luego todas las etapas. Como el CDS es lo más lento, conviene lanzar
primero `entrenar.sh pronosticos` y dejarlo corriendo.

**Resultados:**
- `data/planificador/corridas/principal/metricas.json`
- `ml/planificador/modelos/planificador-<fecha>/`: `model.onnx`, `normalizacion.json`,
  `cultivos.json`, `correccion.npz`, `metricas.json`, `MODELO.md` y `manifiesto.json`. Se versiona.

## 4. Servir en CPU (backend)

```python
from surco_planificador.inferencia import Lote, Planificador
from surco_planificador.catalogo import cargar_fichas

fichas, _, _ = cargar_fichas(catalogo_json, ky_yaml, 365, ["arroz_riego"])
plan = Planificador(Path("ml/planificador/modelos/planificador-2026-11-20"), fichas)
diario = plan.corregir(seas5_interpolado_al_lote, inicio=date(2026, 11, 1), lote=lote)
ranking = plan.recomendar(lote, diario, inicio=date(2026, 11, 1), siembra=date(2026, 11, 20),
                          oni=-0.6, oni_tendencia=-0.2)
```

Solo necesita `numpy`, `pyyaml` y `onnxruntime` (las dependencias base del paquete). Si
`model_int8.onnx` pasó la prueba al exportar, se usa automáticamente.

### 4.1 Microservicio HTTP (lo que usa el backend)

`python -m surco_planificador.servidor --puerto 8090` (servicio `surco-planificador` de
`compose.yaml`; solo lo llama la API, sin puerto publicado). Biblioteca estándar, sin FastAPI.

| Ruta | Qué hace |
|---|---|
| `GET /salud` | `{"estado": "ok", "modelo": "planificador-2026-11-15", "motor": "bf"}`; lo usa el healthcheck |
| `POST /planificar` | `{"lat", "lon", "riego", "siembra"?}` → panorama de lluvia, ranking por fecha de siembra, mejor ciclo, `historico` y `motor` |

Errores `{"code", "message"}`: 400 solicitud inválida o fuera de Colombia; 422
`PLANIFICADOR_TERRENO_SIN_SUELO` (SoilGrids sin perfil cerca) o `PLANIFICADOR_MES_NO_ENTRENADO`;
502 `PLANIFICADOR_FUENTE_CAIDA` con `"fuente"` (NASA POWER, SoilGrids, Open-Meteo o NOAA).

**Latencias.** La primera consulta de una zona descarga NASA POWER desde 1981, la elevación y
SoilGrids: 1 a 3 minutos según la red (el backend espera hasta 180 s). Con esos datos en la caché de
disco (`HttpCache`, volumen `/datos`), una consulta nueva tarda unos 3 s en CPU (medido en Neiva: 2,7 s,
incluida la tabla `historico`). La misma consulta del mismo día sale de la caché de respuestas
(memoria y disco, `crudo/servicio/planes`) en milisegundos, también después de un reinicio.

**Motor: Bf, no la red.** `SURCO_MOTOR=bf` (por defecto) puntúa con el oráculo agronómico sobre cada
miembro del pronóstico corregido (el baseline Bf de `evaluar.py`); `SURCO_MOTOR=red` usa la red ONNX.
La red no pasó la regla de adopción (ganarle a B0 y a Bf en pinball **y** NDCG@3). En la prueba
2020-2025 del bundle `planificador-2026-11-15`:

| | red | Bf | mejor |
|---|---|---|---|
| pinball del rendimiento | 0,01293 | **0,01239** | Bf |
| cobertura p10-p90 (ideal 0,80) | 0,751 | **0,840** | Bf: la red da intervalos muy estrechos |
| ECE de P(éxito) | 0,0215 | **0,0137** | Bf: probabilidades mejor calibradas |
| NDCG@3 | **0,9617** | 0,9597 | red, por 0,002 |
| acierto del primer lugar | **82,4 %** | 80,8 % | red |

La red ordena un poco mejor, pero sus probabilidades de éxito están peor calibradas y sus intervalos
se quedan cortos. Lo que ve el agricultor ("8 de cada 10 años", el grado de una rotación) sale de
P(éxito), así que la calibración pesa más que 0,002 de NDCG. Bf es además explicable (es el mismo
oráculo de las etiquetas corrido sobre el pronóstico) y la red ya lo recibe como entrada. Se vuelve a
evaluar con cada bundle nuevo: si uno pasa la regla, basta con `SURCO_MOTOR=red`.

**Respuesta.** Cada cultivo trae, además del pronóstico, `apto`/`razon_no_apto` (filtros duros de la
ficha: altitud y pH del suelo), `familia`, `fija_nitrogeno` y `fijacion_n_kg_ha` (punto medio del
rango del catálogo), que usa la rotación. `historico` da, para siembras cada 15 días entre 30 y 540
días desde hoy, la frecuencia de éxito de cada cultivo en los años pasados del propio terreno (el
oráculo sobre el clima que ocurrió): es lo que se sabe de las temporadas 2 y 3 de una rotación, que
caen fuera de los 175 días que cubre el pronóstico.

## 5. Pruebas

```bash
cd ml/planificador && uv run pytest
```

- Unitarias: oráculo, catálogo, corrección de sesgo, interpolación y reglas de fuga.
- `test_extremo_a_extremo.py`: la tubería real con datos sintéticos en CPU (etiquetas → entrenar →
  ablación → evaluar → ONNX → recomendar con 9 miembros). Tarda ~20 s.

## 6. Carpetas

```
ml/planificador/
  INVESTIGACION.md         el estudio: pronóstico vs historia, fuentes, arquitectura, CPU
  entrenar.sh, entrenar_windows.ps1
  pyproject.toml, uv.lock  base (CPU) · extras: datos, entrenamiento, mongo
  configs/                 pipeline.yaml, entrenamiento.yaml, ky_fao33.yaml, puntos.csv
  surco_planificador/
    fuentes/               power, c3s, suelo (SoilGrids), oni, elevacion, http con caché
    limpieza/              calidad, et0 (FAO-56), climatologia (sin fuga)
    catalogo.py            MongoDB/JSON → fichas de cultivo y su vector
    pronostico.py          interpolación, corrección de sesgo, anomalías por péntada
    oraculo.py             FAO-56 + FAO-33 + trapecios por fase del catálogo
    etiquetas.py           verdad, B0 y Bf por punto (reanudable)
    caracteristicas.py     las entradas: una sola definición para entrenar y servir
    datos_gpu.py, modelo.py, entrenar.py, metricas.py, evaluar.py
    exportar.py, inferencia.py, tuberia.py, __main__.py
  tests/
data/planificador/         crudo/, limpio/, pronosticos/, etiquetas/, corridas/, registros/ (no se versiona)
```
