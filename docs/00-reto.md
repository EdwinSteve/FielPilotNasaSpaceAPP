# El reto: Field Shift: Adapting Farms with NASA Data

NASA Space Apps Challenge 2026.

## Enunciado oficial

> Farmers around the world navigate shifting temperatures, changing rainfall patterns, water shortages, extreme weather, and declining soil health. These pressures make it difficult to choose crop rotations that protect fields, conserve water, and support long‑term resilience. Your challenge is to create a decision‑support tool that uses NASA Earth observations along with local soil information, crop characteristics, and farmer priorities to help farmers explore rotation strategies that could strengthen soil health and adapt their farms to changing conditions.

## Lo que la herramienta debe combinar

| Entrada que pide el reto | Dónde vive en SURCO |
|---|---|
| Observaciones de la Tierra de la NASA | `ml/datos_tierra` + módulo `diagnostico` |
| Información local del suelo | SoilGrids en `diagnostico` |
| Características de los cultivos | módulo `catalogo` (`data/catalogo_cultivos/`) |
| Explorar estrategias de rotación | módulo `rotacion` (planificador + simulador de escenarios) |
| Adaptarse a condiciones cambiantes | `rotacion/domain/condiciones.py` (pronóstico IDEAM + predicción estacional) y módulo `alertas` |
| Prioridades del agricultor | El objetivo es fijo: el ciclo productivo con mejor resultado bajo el clima esperado. El agricultor aporta su lote, si tiene riego y desde cuándo puede sembrar (`fincas`) |
| Fortalecer la salud del suelo | `rotacion/domain/suelo.py`: efecto de cada temporada sobre el suelo |

## Ejes que la solución aborda

- Conservación de agua (riego frente a agotamiento de acuíferos)
- Reposición biológica de nitrógeno (leguminosas)
- Ruptura de ciclos de plagas y patógenos (diversidad de familias botánicas)
- Resiliencia a calor extremo y sequía (incluye alertas tempranas del IDEAM)

## Entregables

- [ ] Página del proyecto en el sitio de Space Apps
- [ ] Repositorio público (open source) con enlace desde la página
- [ ] Video corto (lo clave debe entenderse en los primeros 30 s)
- [ ] Demo o prototipo funcional

## Criterios de evaluación

Impact · Creativity · Validity · Relevance · Presentation.
