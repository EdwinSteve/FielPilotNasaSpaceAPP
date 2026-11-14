# 0006: Alertas de lluvia y sequía con pronósticos del IDEAM

- Estado: propuesta
- Fecha: 2026-11-14

## Contexto
Los datos de la NASA describen muy bien el clima que ya pasó y el estado actual del lote, pero el agricultor también necesita saber qué viene: lluvias por encima de lo normal o una sequía en su municipio. En Colombia, la fuente oficial de pronósticos y alertas hidrometeorológicas es el IDEAM, a través de su Oficina del Servicio de Pronóstico y Alertas (OSPA).

El IDEAM publica en varios formatos, y no todos se consultan por API:
- **Datos abiertos en datos.gov.co** (API Socrata): sobre todo observaciones de estaciones (precipitación, temperatura, humedad).
- **Servicios GIS del visualizador del IDEAM** (ArcGIS REST), por ejemplo la precipitación acumulada.
- **Boletines e informes en PDF**: predicción climática mensual y trimestral, y boletines de alertas hidrológicas.

## Decisión
- Un módulo nuevo, `alertas`, con un puerto `FuentePronosticos` que oculta de dónde vienen los pronósticos. El adaptador `infrastructure/ideam.py` empieza por la fuente que se pueda consultar de forma estable y suma las demás después.
- Una tarea periódica (`backend/scripts/revisar_alertas_ideam.py`, cada 6 h) trae los pronósticos, los cruza con el municipio o el polígono de cada lote y genera alertas de **lluvia excesiva** o **sequía** con nivel amarillo, naranja o rojo.
- Las notificaciones salen por un puerto `CanalNotificacion`. El primer canal es la app; SMS o WhatsApp se agregan como adaptadores nuevos.
- Cada alerta guarda su fuente, su vigencia y el boletín de origen, y la misma alerta no se notifica dos veces al mismo productor.

## Pendiente de verificar
- Qué conjunto o servicio del IDEAM trae el pronóstico de lluvia y sequía por municipio con la menor latencia.
- Si la predicción climática mensual solo existe en PDF, cómo extraerla de forma confiable o si se usa la capa GIS equivalente.

## Consecuencias
- El agricultor recibe avisos oficiales sobre su finca, no solo datos históricos.
- Agregar otra fuente de pronósticos (por ejemplo, NOAA o ECMWF) solo exige un adaptador nuevo.
- Depende de la disponibilidad de los servicios del IDEAM: la tarea debe tolerar fallos y reintentar sin duplicar alertas.
