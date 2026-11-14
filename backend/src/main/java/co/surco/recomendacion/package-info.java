/**
 * Módulo recomendacion (por construir). Cultivos aptos para el lote y la fecha de siembra según el modelo predictivo (XGBoost, {@code ml/surco_ml}).
 *
 * <p>Idoneidad, probabilidad de éxito ('8 de cada 10 años'), riego necesario y días de estrés de cada cultivo.
 *
 * <p>Estructura, igual que el módulo identidad:
 * <ul>
 *   <li>{@code co.surco.recomendacion}: API pública (interfaces y records que otros módulos pueden usar).</li>
 *   <li>{@code dominio}: entidades, reglas y puertos. Java puro, sin Spring ni JPA.</li>
 *   <li>{@code aplicacion}: casos de uso ({@code @Service}, {@code @Transactional}).</li>
 *   <li>{@code infraestructura}: adaptadores (JPA en el esquema "recomendacion", clientes HTTP...).</li>
 *   <li>{@code api}: controladores REST bajo /api/v1, protegidos por defecto con JWT.</li>
 * </ul>
 * Ver backend/README.md, sección "Cómo proteger las rutas de un módulo nuevo".
 */
package co.surco.recomendacion;
