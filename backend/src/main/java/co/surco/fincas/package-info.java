/**
 * Módulo fincas: los terrenos (lotes) del productor, segmentados sobre el mapa satelital del frontend.
 *
 * <p>Tabla {@code fincas.terrenos} (migración V4), relacionada con {@code identidad.usuarios} por
 * {@code usuario_id} con borrado en cascada. Guarda el polígono en GeoJSON, el área en hectáreas,
 * el centroide (el punto que usan diagnóstico y el planificador estacional), el municipio y el riego.
 *
 * <p>Rutas protegidas bajo {@code /api/v1/fincas/terrenos} (POST, GET, GET /{id}, DELETE /{id}): cada
 * productor solo ve sus terrenos y uno ajeno responde 404, igual que uno inexistente. El municipio se
 * valida con {@link co.surco.territorio.ConsultaTerritorio}; el frontend lo sugiere con
 * {@code GET /api/v1/territorio/municipios/cercano}.
 *
 * <p>Otros módulos usan {@link co.surco.fincas.ConsultaTerrenos} y {@link co.surco.fincas.UbicacionTerreno}.
 *
 * <p>Estructura, igual que el módulo identidad:
 * <ul>
 *   <li>{@code co.surco.fincas}: API pública (interfaces y records que otros módulos pueden usar).</li>
 *   <li>{@code dominio}: Terreno, Poligono (validación, área, centroide, GeoJSON) y puertos. Java puro.</li>
 *   <li>{@code aplicacion}: RegistrarTerreno, ConsultasTerrenos, EliminarTerreno.</li>
 *   <li>{@code infraestructura}: JPA en el esquema "fincas" y el validador de municipios.</li>
 *   <li>{@code api}: TerrenoControlador y sus DTOs.</li>
 * </ul>
 */
package co.surco.fincas;
