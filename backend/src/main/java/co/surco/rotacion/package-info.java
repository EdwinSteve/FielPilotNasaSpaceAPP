/**
 * Módulo rotacion (por construir). El núcleo del reto: recomienda los ciclos productivos (cultivo + fecha de siembra, en secuencias de 2-3 temporadas) con el mejor resultado esperado bajo las condiciones meteorológicas que se van a presentar en el lote.
 *
 * <p>Condiciones esperadas = pronóstico IDEAM + predicción estacional + climatología. Filtros duros (familia botánica, altitud, pH, agua en secano, calor) y puntaje MCDA 0-100 con pesos fijos (ADR 0001).
 * <p>Incluye el efecto de cada temporada en la salud del suelo y el simulador de escenarios (+0 a +4 °C, -40 % a +20 % de lluvia).
 *
 * <p>Estructura, igual que el módulo identidad:
 * <ul>
 *   <li>{@code co.surco.rotacion}: API pública (interfaces y records que otros módulos pueden usar).</li>
 *   <li>{@code dominio}: entidades, reglas y puertos. Java puro, sin Spring ni JPA.</li>
 *   <li>{@code aplicacion}: casos de uso ({@code @Service}, {@code @Transactional}).</li>
 *   <li>{@code infraestructura}: adaptadores (JPA en el esquema "rotacion", clientes HTTP...).</li>
 *   <li>{@code api}: controladores REST bajo /api/v1, protegidos por defecto con JWT.</li>
 * </ul>
 * Ver backend/README.md, sección "Cómo proteger las rutas de un módulo nuevo".
 */
package co.surco.rotacion;
