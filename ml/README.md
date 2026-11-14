# ml

Dos paquetes de Python que el backend instala en modo editable (`pip install -e "../ml[gee,modelo]"`):

| Paquete | Qué hace |
|---|---|
| `datos_tierra/` | Descarga y unifica los datos de un punto: NASA POWER, SMAP, IMERG, MODIS, SoilGrids, DEM SRTM y ENSO (ONI). |
| `surco_ml/` | Modelo predictivo de idoneidad (modelo A, XGBoost): dataset, oráculo agronómico, entrenamiento y `predecir`. |

Los modelos `.ubj` no se versionan; solo `informe.json` y `calibracion.json`.
