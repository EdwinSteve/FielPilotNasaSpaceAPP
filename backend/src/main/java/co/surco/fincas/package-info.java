/**
 * Módulo fincas (por construir). Fincas y lotes del productor: polígono, área, centroide y tipo de riego.
 *
 * <p>Rutas protegidas: cada productor solo ve sus fincas (filtrar siempre por {@code usuario.id()}; un recurso de otro responde 404, igual que uno inexistente).
 * <p>Valida el municipio con {@link co.surco.territorio.ConsultaTerritorio} (en el frontend, el componente SelectorUbicacion).
 *
 * <p>Estructura, igual que el módulo identidad:
 * <ul>
 *   <li>{@code co.surco.fincas}: API pública (interfaces y records que otros módulos pueden usar).</li>
 *   <li>{@code dominio}: entidades, reglas y puertos. Java puro, sin Spring ni JPA.</li>
 *   <li>{@code aplicacion}: casos de uso ({@code @Service}, {@code @Transactional}).</li>
 *   <li>{@code infraestructura}: adaptadores (JPA en el esquema "fincas", clientes HTTP...).</li>
 *   <li>{@code api}: controladores REST bajo /api/v1, protegidos por defecto con JWT.</li>
 * </ul>
 * Ver backend/README.md, sección "Cómo proteger las rutas de un módulo nuevo".
 */
package co.surco.fincas;
