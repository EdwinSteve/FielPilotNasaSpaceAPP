package co.surco.identidad.dominio;

/**
 * Celular móvil colombiano. Acepta "300 123 4567", "+57 300-123-4567" o "573001234567"
 * y siempre guarda los 10 dígitos ("3001234567").
 */
public record Celular(String valor) {

	public Celular {
		if (valor == null || !valor.matches("3\\d{9}")) {
			throw ErroresIdentidad.celularInvalido();
		}
	}

	public static Celular de(String texto) {
		if (texto == null) {
			throw ErroresIdentidad.celularInvalido();
		}
		String digitos = texto.replaceAll("[\\s\\-().]", "");
		if (digitos.startsWith("+57")) {
			digitos = digitos.substring(3);
		} else if (digitos.length() == 12 && digitos.startsWith("57")) {
			digitos = digitos.substring(2);
		}
		return new Celular(digitos);
	}

	/** Formato internacional E.164, para enviar SMS: +573001234567. */
	public String internacional() {
		return "+57" + valor;
	}
}
