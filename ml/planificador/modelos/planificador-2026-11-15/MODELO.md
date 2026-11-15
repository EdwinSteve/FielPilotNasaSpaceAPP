# PlanificadorEstacionalNet

**No adoptado:** no le gana a B0 y a Bf; el backend sirve Bf.

- **Tarea:** rendimiento relativo en secano y con riego (p10/p50/p90), P(rendimiento >= 0,6),
  riego necesario y días de estrés térmico para 23 cultivos del catálogo surco_agro.
- **Entradas:** 365 días de NASA POWER antes de sembrar, el pronóstico ECMWF SEAS5 corregido
  (conjunto de miembros, 175 días), la climatología sin fuga, el suelo (SoilGrids) y el ENSO.
- **Etiquetas:** oráculo FAO-56 / FAO-33 / trapecios del catálogo sobre el clima que ocurrió.
- **No está calibrado contra cosechas reales.** Las fichas del catálogo no las ha validado un agrónomo.
- **Paridad ONNX:** 5.72e-06. **CPU:** 11.53 ms por lote (fp32);
  INT8 descartado (9.95 ms).

## Prueba (2020-2025, solo pronósticos operativos)

| Modelo | rend_pinball | rend_mae | rend_cobertura_p10_p90 | exito_brier | exito_ece | riego_mae_mm | rend_riego_pinball | ndcg_3 | acierto_top1 |
|---|---|---|---|---|---|---|---|---|---|
| red | 0.01293 | 0.03645 | 0.7507 | 0.0315 | 0.02148 | 59.57 | 0.01133 | 0.96169 | 0.8245 |
| b0_climatologia | 0.02491 | 0.04982 | 0.5775 | 0.05693 | 0.05693 | 81.48 | 0.02214 | 0.92476 | 0.7706 |
| bf_pronostico | 0.01239 | 0.03766 | 0.8401 | 0.03074 | 0.01374 | 62.76 | 0.01728 | 0.95973 | 0.8082 |
| b1_historia | 0.01246 | 0.03799 | 0.8361 | 0.03127 | 0.01377 | 68.24 | 0.01693 | 0.95542 | 0.798 |
| red_sin_pronostico | 0.01385 | 0.03825 | 0.7516 | 0.03474 | 0.02717 | 64.65 | 0.01168 | 0.95828 | 0.8122 |

## Prueba espacial (Huila nunca visto)

Sin correr.
