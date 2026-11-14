package co.surco.compartido.error;

/** La API usa snake_case en el JSON; los records de Java, camelCase. */
public final class NombresJson {

	private NombresJson() {
	}

	/** {@code nombreCompleto} → {@code nombre_completo}. */
	public static String aSnakeCase(String camelCase) {
		return camelCase.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
	}
}
