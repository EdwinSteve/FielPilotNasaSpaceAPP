# 0008: Recomendación con Bf y rotaciones de 2-3 temporadas encima del modelo

- Estado: aceptada
- Fecha: 2026-11-15

## Contexto
El planificador estacional (`ml/planificador`) ya estaba entrenado y servido por HTTP, pero la pantalla de rotación del frontend era 100 % de ejemplo y el backend no guardaba nada. Había que decidir tres cosas:

1. **Qué motor servir.** El `MODELO.md` del bundle `planificador-2026-11-15` dice "no adoptado: no le gana a B0 y a Bf", pero el servidor servía la red.
2. **Cómo convertir lo que da el modelo en un puntaje 0-1** para el ranking que ve el agricultor.
3. **Cómo armar rotaciones de 2-3 temporadas**, si el modelo puntúa una sola siembra y su pronóstico cubre 175 días.

## Decisión

### 1. El planificador sirve Bf (`SURCO_MOTOR=bf`)
Bf es el oráculo agronómico (FAO-56, FAO-33 y los trapecios del catálogo) corrido sobre cada miembro del pronóstico corregido. En la prueba 2020-2025:

| | red | Bf |
|---|---|---|
| pinball del rendimiento | 0,01293 | **0,01239** |
| cobertura p10-p90 (ideal 0,80) | 0,751 | **0,840** |
| ECE de P(éxito) | 0,0215 | **0,0137** |
| NDCG@3 | **0,9617** | 0,9597 |

La red ordena apenas mejor, pero sus probabilidades están peor calibradas y sus intervalos son estrechos. Lo que ve el agricultor ("8 de cada 10 años", el grado) sale de P(éxito), así que pesa la calibración. Bf además es explicable y la red ya lo recibe como entrada, así que servirlo no cuesta nada. Cada respuesta dice qué motor la produjo; con un bundle que pase la regla de adopción basta `SURCO_MOTOR=red`.

### 2. Puntaje del ranking (`recomendacion.dominio.Puntaje`)
- **Secano:** `puntaje = (prob_exito + rend_p50) / 2`. Premia por igual que el cultivo salga bien casi todos los años y cuánto rinde en el año típico.
- **Con riego:** `puntaje = rend_con_riego_p50`. Con riego el agua deja de limitar.

Se recorta a [0, 1]. Los **filtros duros** (altitud y pH del suelo frente a la ficha del catálogo) los aplica el planificador, que es quien tiene el catálogo; un cultivo no apto va al final con su razón. El ranking solo trae los 23 cultivos que puntúa el modelo estacional: nunca perennes como café o cacao.

### 3. Rotaciones v1 (`rotacion.dominio.GeneradorRotaciones`)
- **Temporada 1:** los 6 cultivos aptos de mayor puntaje, en la fecha de siembra recomendada, con la P(éxito) del pronóstico.
- **Temporadas 2 y 3:** se siembra 15 días después de la cosecha anterior. Esas fechas caen fuera del pronóstico, así que su P(éxito) es la **frecuencia de éxito del cultivo en los años pasados del propio terreno** (el oráculo sobre el clima que ocurrió, como el baseline B1), que el planificador calcula para siembras cada 15 días hasta 540 días (`historico`). Fuera de esa tabla la secuencia termina.
- **Reglas:** solo cultivos aptos, sin repetir cultivo y nunca la misma familia botánica en dos temporadas seguidas.
- **Puntaje** = media de P(éxito) + 0,05 si una leguminosa fijadora va antes de otro cultivo (recortado a 1). **Grado:** A+ ≥ 0,85; A ≥ 0,70; B ≥ 0,55; C ≥ 0,40; D. **Frecuencia** = round(P media × 10), "le va bien N de cada 10 años".
- **Agua ahorrada:** frente al monocultivo de referencia, que es sembrar una y otra vez el cultivo comercial apto de mayor puntaje (un abono verde no cuenta: no se vende) en las mismas fechas. Riego de la referencia menos riego de la secuencia (mm del año típico) × área × 10 m³/(mm·ha), nunca negativa.
- **Aporte de nitrógeno:** la primera leguminosa que va antes de otro cultivo, con los kg N/ha que fija según el catálogo (punto medio del rango).
- **Top 3** por puntaje, sin repetir las dos primeras temporadas, para que las opciones sean distintas de verdad.

La elección del agricultor (`PUT /api/v1/lotes/{id}/seleccion`) apunta a un plan guardado: aunque se recalcule, lo elegido no cambia.

**Alternativas descartadas:**
- *Llamar al modelo por cada temporada:* el pronóstico y la historia de 365 días que necesita no existen todavía para siembras dentro de 6-12 meses.
- *Climatología (B0) para las temporadas 2 y 3:* con un solo "año promedio" la P(éxito) sale 0 o 1. La frecuencia en años reales da probabilidades y es la misma frase que entiende el agricultor.
- *Referencia del agua = el primer cultivo de cada rotación:* con un abono verde de primero (pide poco riego) ninguna rotación "ahorra" nada.

## Consecuencias
- Las temporadas 2 y 3 no usan el pronóstico: un año Niño o Niña muy marcado solo pesa en la primera. La pantalla dice de dónde sale cada temporada (`fuente`: `pronostico` o `historico`).
- La frecuencia de éxito es en secano; en un terreno con riego el beneficio aparece en el agua ahorrada, no en la frecuencia.
- Nada de esto está calibrado contra cosechas reales (lo dice el `MODELO.md`); las fichas del catálogo no las ha validado un agrónomo.
- En zonas cálidas la crotalaria (abono verde) puede salir de primera: es una recomendación legítima para el suelo, pero no da ingreso. La pantalla la muestra con su nombre del catálogo, "Crotalaria (abono verde)".
