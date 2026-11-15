/**
 * Módulo clima: condiciones actuales de un punto para las tarjetas del tablero.
 *
 * <p>{@code GET /api/v1/weather/current?lat=&lon=} (protegida): seis tarjetas, siempre en este orden:
 * temperatura del aire, precipitación de 24 h, humedad relativa y viento (Weather API de Google, casi en
 * tiempo real), y radiación solar y humedad del suelo (NASA POWER, dato diario con unos días de retraso,
 * porque Google no las entrega). Si una fuente falla, solo sus tarjetas llegan con {@code valor} null y
 * "Sin dato reciente"; el endpoint no responde error. La respuesta se guarda 10 minutos por punto.
 *
 * <p>La clave de Google va en la variable {@code GOOGLE_WEATHER_API_KEY} (backend/.env).
 *
 * <p>Estructura, igual que el módulo identidad:
 * <ul>
 *   <li>{@code dominio}: las tarjetas (textos, unidades, rumbo del viento), puertos y errores. Java puro.</li>
 *   <li>{@code aplicacion}: ConsultarClimaActual.</li>
 *   <li>{@code infraestructura}: ClienteGoogleWeather y ClienteNasaPowerDiario (HTTP).</li>
 *   <li>{@code api}: ClimaControlador.</li>
 * </ul>
 */
package co.surco.clima;
