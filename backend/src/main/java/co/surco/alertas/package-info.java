/**
 * Módulo alertas (por construir). Pronósticos del IDEAM de lluvia y sequía cruzados con la ubicación de cada finca, y notificaciones al agricultor (ADR 0006).
 *
 * <p>Tarea periódica cada 6 h. Expone el pronóstico vigente del lote a rotacion por su API pública.
 *
 * <p>Estructura, igual que el módulo identidad:
 * <ul>
 *   <li>{@code co.surco.alertas}: API pública (interfaces y records que otros módulos pueden usar).</li>
 *   <li>{@code dominio}: entidades, reglas y puertos. Java puro, sin Spring ni JPA.</li>
 *   <li>{@code aplicacion}: casos de uso ({@code @Service}, {@code @Transactional}).</li>
 *   <li>{@code infraestructura}: adaptadores (JPA en el esquema "alertas", clientes HTTP...).</li>
 *   <li>{@code api}: controladores REST bajo /api/v1, protegidos por defecto con JWT.</li>
 * </ul>
 * Ver backend/README.md, sección "Cómo proteger las rutas de un módulo nuevo".
 */
package co.surco.alertas;
