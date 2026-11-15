# Búsqueda de hiperparámetros (15-nov-2026)

Etapa `ajustar` (16 pruebas × 12 épocas, corrida `ajustada`) y reentrenamiento completo con la mejor
configuración. **Se conserva este bundle** (configuración por defecto de `configs/entrenamiento.yaml`):
el ajustado empata en casi todo y queda un poco peor en lo que usa la app, que es acertar el mejor
cultivo y ordenar el ranking.

Validación de la búsqueda (pérdida, menor es mejor): mejor prueba 0,1557 (prueba 15), frente a 0,1553
del modelo por defecto. El modelo final ajustado llegó a 0,1593 en validación.

| Prueba 2020–2025 (años no vistos) | Por defecto (este bundle) | Ajustado |
|---|---|---|
| Acierto del mejor cultivo (top-1) | **0,8245** | 0,8210 |
| Calidad del ranking (NDCG@3) | **0,9617** | 0,9610 |
| Error de rendimiento (pinball) | **0,01293** | 0,01296 |
| Error medio de rendimiento (MAE) | 0,0365 | **0,0362** |
| Cobertura p10–p90 (ideal 0,80) | **0,751** | 0,747 |
| Probabilidad de éxito (Brier) | **0,0315** | 0,0316 |
| Calibración del éxito (ECE) | **0,0215** | 0,0218 |
| Error de riego (mm) | 59,6 | **59,4** |

Ninguno de los dos pasa la regla de adopción de `evaluar.adoptada` (ganarle a Bf, la simulación física
de cada miembro del pronóstico, también en el error de rendimiento: Bf tiene 0,01239). La red sí le
gana a Bf y a la climatología en el ranking de cultivos (top-1 0,808 y 0,771).

Siguiente paso con más probabilidad de mejorar la calibración: entrenar con SEAS5 (`fuente: c3s`)
cuando la cola del CDS lo permita, o con más años de análogos (desde 1982).
