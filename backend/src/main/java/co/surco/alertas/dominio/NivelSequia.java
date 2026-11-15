package co.surco.alertas.dominio;

/**
 * Nivel del indicador de sequía según cuánto ha llovido frente a lo normal, y qué decirle al agricultor.
 * Los umbrales son una propuesta del equipo, no un estándar oficial.
 */
public enum NivelSequia {

	NORMAL(80, "La lluvia del último mes ha sido normal en tu zona."),
	AMARILLA(60, "Ha llovido menos de lo normal en el último mes. Cuida el agua y revisa la humedad de tu suelo."),
	NARANJA(40, "Ha llovido mucho menos de lo normal en el último mes. "
		+ "Riega solo lo necesario y cubre el suelo para que no pierda humedad."),
	ROJA(0, "Sequía fuerte: casi no ha llovido en el último mes. "
		+ "Asegura agua para tus animales y, si puedes, aplaza las siembras nuevas.");

	private final double desdePorcentaje;
	private final String mensaje;

	NivelSequia(double desdePorcentaje, String mensaje) {
		this.desdePorcentaje = desdePorcentaje;
		this.mensaje = mensaje;
	}

	public String mensaje() {
		return mensaje;
	}

	/** 100 = llovió lo normal; 50 = llovió la mitad de lo normal. */
	public static NivelSequia desdePorcentaje(double porcentaje) {
		for (NivelSequia nivel : values()) {
			if (porcentaje >= nivel.desdePorcentaje) {
				return nivel;
			}
		}
		return ROJA;
	}
}
