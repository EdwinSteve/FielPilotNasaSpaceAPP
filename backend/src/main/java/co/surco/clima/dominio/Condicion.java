package co.surco.clima.dominio;

/**
 * Una tarjeta del tablero.
 *
 * @param variable TEMPERATURA_AIRE, PRECIPITACION_24H, HUMEDAD_RELATIVA, VIENTO, RADIACION_SOLAR o HUMEDAD_SUELO
 * @param valor    null cuando la fuente no tiene dato reciente
 * @param medido   cuándo se tomó el dato: fecha y hora (Google) o solo el día (NASA POWER)
 */
public record Condicion(String variable, Double valor, String unidad, String detalle, String fuente, String medido) {

	static Condicion sinDato(String variable, String unidad, String fuente) {
		return new Condicion(variable, null, unidad, "Sin dato reciente", fuente, null);
	}

	public boolean tieneDato() {
		return valor != null;
	}
}
