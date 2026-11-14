package co.surco.identidad.dominio;

import java.util.Locale;

/** Correo electrónico, en minúsculas para que "Ana@x.co" y "ana@x.co" sean el mismo. */
public record Correo(String valor) {

	public Correo {
		if (valor == null || valor.length() > 254 || !valor.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
			throw ErroresIdentidad.correoInvalido();
		}
	}

	/** Correo opcional: vacío o en blanco = sin correo. */
	public static Correo deOpcional(String texto) {
		if (texto == null || texto.isBlank()) {
			return null;
		}
		return new Correo(texto.trim().toLowerCase(Locale.ROOT));
	}
}
