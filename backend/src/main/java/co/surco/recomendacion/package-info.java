/**
 * Módulo recomendacion: el mejor ciclo de cultivo de un terreno con el clima que se espera.
 *
 * <p>{@code GET /api/v1/recomendacion/terrenos/{id}} (protegida): toma el centroide y el riego del
 * terreno con la API pública de fincas y llama al planificador estacional
 * ({@code ml/planificador}, {@code python -m surco_planificador.servidor}, servicio
 * {@code surco-planificador} de compose.yaml, URL en {@code surco.planificador.url}). El planificador
 * trae el clima diario del punto (NASA POWER), el suelo (SoilGrids) y el estado de El Niño, arma el
 * pronóstico de los próximos meses y corre la red ONNX. Responde el panorama de lluvia y temperatura,
 * el ranking de cultivos para varias fechas de siembra del próximo mes y el mejor ciclo.
 *
 * <p>Si el planificador no responde, 503 {@code RECOMENDACION_NO_DISPONIBLE}; la primera consulta de una
 * zona puede tardar 1-2 minutos ({@code surco.planificador.tiempo-maximo}).
 *
 * <p>Estructura, igual que el módulo identidad:
 * <ul>
 *   <li>{@code dominio}: PlanCiclo (la respuesta del modelo), puertos y errores. Java puro.</li>
 *   <li>{@code aplicacion}: RecomendarCiclo.</li>
 *   <li>{@code infraestructura}: ClientePlanificador (HTTP) y TerrenosFincas (API pública de fincas).</li>
 *   <li>{@code api}: RecomendacionControlador.</li>
 * </ul>
 */
package co.surco.recomendacion;
