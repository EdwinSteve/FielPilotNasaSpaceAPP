/**
 * Módulo clima. El tiempo de hoy, el pronóstico de 7 días y los avisos de un punto de Colombia:
 * lo que muestra la pantalla de inicio del agricultor ({@code GET /api/v1/clima/resumen}).
 *
 * <p>Fuentes, cada una detrás de un puerto del dominio:
 * <ul>
 *   <li>Open-Meteo: condiciones actuales y pronóstico diario (no necesita clave).</li>
 *   <li>NASA POWER: radiación solar, humedad del suelo en la zona de raíces y lluvia del último mes
 *       frente a lo normal (indicador de sequía).</li>
 *   <li>IDEAM: alerta hidrológica vigente de la subzona donde cae el punto.</li>
 * </ul>
 * Solo el pronóstico es obligatorio: si NASA POWER o el IDEAM no responden, esa parte llega vacía.
 *
 * <p>Los clientes de NASA POWER y del IDEAM y el cálculo de sequía vienen de la rama
 * {@code feature/backend}, adaptados a las capas de este monolito. El módulo {@code alertas} ya expone
 * la alerta del IDEAM y el indicador de sequía para la pantalla /alertas (GET /api/v1/alerts/flood y
 * /alerts/drought) con sus propios clientes. Pendiente: que este módulo los consulte por la API pública
 * de {@code alertas} en vez de repetir los clientes.
 *
 * <p>No tiene tablas: guarda cada respuesta 10 minutos en memoria.
 */
package co.surco.clima;
