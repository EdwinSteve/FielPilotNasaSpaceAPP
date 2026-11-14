/**
 * Módulo diagnostico (por construir). Resumen de la información climatológica disponible para el lote: lluvia y temperatura históricas (NASA POWER, IMERG), humedad del suelo (SMAP), verdor (MODIS/HLS) y suelo (SoilGrids).
 *
 * <p>Cada dato lleva su fuente y fecha. Los datos los entrega el servicio Python {@code ml/datos_tierra} (adaptador en infraestructura).
 *
 * <p>Estructura, igual que el módulo identidad:
 * <ul>
 *   <li>{@code co.surco.diagnostico}: API pública (interfaces y records que otros módulos pueden usar).</li>
 *   <li>{@code dominio}: entidades, reglas y puertos. Java puro, sin Spring ni JPA.</li>
 *   <li>{@code aplicacion}: casos de uso ({@code @Service}, {@code @Transactional}).</li>
 *   <li>{@code infraestructura}: adaptadores (JPA en el esquema "diagnostico", clientes HTTP...).</li>
 *   <li>{@code api}: controladores REST bajo /api/v1, protegidos por defecto con JWT.</li>
 * </ul>
 * Ver backend/README.md, sección "Cómo proteger las rutas de un módulo nuevo".
 */
package co.surco.diagnostico;
