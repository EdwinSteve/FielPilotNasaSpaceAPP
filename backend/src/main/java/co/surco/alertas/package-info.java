/**
 * Módulo alertas: lo que le avisa al agricultor sobre el agua en su terreno.
 *
 * <ul>
 *   <li>{@code GET /api/v1/alerts/flood?lat=&lon=} (protegida): alerta hidrológica vigente del IDEAM para la
 *   subzona hidrográfica del punto. La capa del IDEAM solo trae subzonas con alerta, así que si el punto no
 *   aparece responde {@code SIN_ALERTA} (después de comprobar que el punto está en Colombia).</li>
 *   <li>{@code GET /api/v1/alerts/drought?lat=&lon=} (protegida): indicador propio de sequía con la lluvia de
 *   NASA POWER: lluvia de los últimos 30 días frente a la normal de esos días. No es una alerta oficial.</li>
 * </ul>
 *
 * <p>Errores (contrato con frontend/src/modulos/alertas): 400 {@code UBICACION_FUERA_DE_COLOMBIA},
 * 502 {@code NIVEL_DESCONOCIDO} (el IDEAM envió un nivel que no conocemos) y 503 {@code FUENTE_NO_DISPONIBLE}.
 *
 * <p>Estructura, igual que el módulo identidad:
 * <ul>
 *   <li>{@code dominio}: niveles, cálculo del indicador de sequía, puertos y errores. Java puro.</li>
 *   <li>{@code aplicacion}: ConsultarAlertas.</li>
 *   <li>{@code infraestructura}: ClienteIdeam (servidor GIS del IDEAM) y ClienteNasaPower (HTTP).</li>
 *   <li>{@code api}: AlertasControlador.</li>
 * </ul>
 */
package co.surco.alertas;
