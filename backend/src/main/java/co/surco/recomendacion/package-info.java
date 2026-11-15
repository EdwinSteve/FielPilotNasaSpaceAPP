/**
 * Módulo recomendacion: qué cultivos le convienen a un terreno con el clima que se espera.
 *
 * <p>Rutas (protegidas, no están en RutasPublicas):
 * <ul>
 *   <li>{@code POST /api/v1/lotes/{id}/recomendaciones?siembra=AAAA-MM-DD}: calcula con el planificador y
 *   guarda en el esquema "recomendacion" (migración V5). Sin {@code siembra} compara las fechas del
 *   próximo mes.</li>
 *   <li>{@code GET /api/v1/lotes/{id}/recomendaciones/actual} y {@code .../{recId}}: las guardadas.</li>
 *   <li>{@code GET /api/v1/recomendacion/terrenos/{id}}: el plan completo sin guardar (pantalla
 *   /lotes/[loteId]).</li>
 * </ul>
 *
 * <p>Toma el centroide y el riego del terreno con la API pública de fincas y llama al planificador
 * estacional ({@code ml/planificador}, servicio {@code surco-planificador} de compose.yaml, URL en
 * {@code surco.planificador.url}), que puntúa con el motor Bf (oráculo sobre el pronóstico; ver
 * ml/planificador/README.md). El puntaje 0-1 del ranking está en {@code dominio.Puntaje}; los filtros
 * duros (altitud y pH) los aplica el planificador con las fichas del catálogo.
 *
 * <p>Errores: 404 terreno ajeno o inexistente; 422 terreno sin suelo en SoilGrids o mes no entrenado;
 * 502 fuente caída (con {@code details[0].message} = la fuente); 503 si el planificador no responde.
 * La primera consulta de una zona puede tardar 1-3 minutos ({@code surco.planificador.tiempo-maximo}).
 *
 * <p>API pública para otros módulos: {@link co.surco.recomendacion.ConsultaRecomendaciones} (la usa rotacion).
 *
 * <p>Estructura, igual que el módulo identidad:
 * <ul>
 *   <li>{@code dominio}: PlanCiclo (la respuesta del modelo), Recomendacion, Puntaje, Lluvias, puertos y
 *   errores. Java puro.</li>
 *   <li>{@code aplicacion}: RecomendarCiclo y CalcularRecomendacion (implementa la API pública).</li>
 *   <li>{@code infraestructura}: ClientePlanificador (HTTP), TerrenosFincas (API pública de fincas) y
 *   persistencia (JPA).</li>
 *   <li>{@code api}: RecomendacionControlador y RecomendacionesLoteControlador.</li>
 * </ul>
 */
package co.surco.recomendacion;
