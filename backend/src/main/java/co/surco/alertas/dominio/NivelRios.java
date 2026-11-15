package co.surco.alertas.dominio;

import java.util.Locale;

/** Nivel de la alerta hidrológica del IDEAM (campo NIVEL_A de la capa Alertas_vigentes) y qué decirle al agricultor. */
public enum NivelRios {

	/** La subzona no tiene alerta vigente: el IDEAM no la incluye en la capa. */
	SIN_ALERTA(null, "El IDEAM no tiene alertas vigentes para los ríos y quebradas de tu zona."),
	NORMAL("CONDICIONES NORMALES", "Sin alertas por ahora en los ríos y quebradas de tu zona."),
	AMARILLA("ALERTA AMARILLA", "Atención: el IDEAM está vigilando los ríos y quebradas de tu zona. "
		+ "Todavía no hay peligro, pero mantente pendiente."),
	NARANJA("ALERTA NARANJA", "Prepárate: los ríos y quebradas de tu zona pueden crecer pronto. "
		+ "Aleja de la orilla animales, bombas y herramientas."),
	ROJA("ALERTA ROJA", "Peligro: hay riesgo alto de creciente o inundación en tu zona. "
		+ "Aléjate de ríos y quebradas y pon a salvo a tu familia y tus animales.");

	private final String valorIdeam;
	private final String mensaje;

	NivelRios(String valorIdeam, String mensaje) {
		this.valorIdeam = valorIdeam;
		this.mensaje = mensaje;
	}

	public String mensaje() {
		return mensaje;
	}

	/** {@code "ALERTA NARANJA"} → NARANJA. Un valor que no conocemos es un error 502, nunca NORMAL. */
	public static NivelRios desdeIdeam(String nivelA) {
		String limpio = nivelA == null ? "" : nivelA.trim().toUpperCase(Locale.ROOT);
		for (NivelRios nivel : values()) {
			if (limpio.equals(nivel.valorIdeam)) {
				return nivel;
			}
		}
		throw ErroresAlertas.nivelDesconocido();
	}
}
