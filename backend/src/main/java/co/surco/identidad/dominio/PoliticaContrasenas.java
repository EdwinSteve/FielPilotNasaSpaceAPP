package co.surco.identidad.dominio;

/** Reglas de la contraseña: 8 a 128 caracteres, con al menos una letra y un número. */
public final class PoliticaContrasenas {

	public static final int MINIMO = 8;
	public static final int MAXIMO = 128;

	private PoliticaContrasenas() {
	}

	public static void validar(String contrasena) {
		if (contrasena == null
			|| contrasena.length() < MINIMO
			|| contrasena.length() > MAXIMO
			|| !contrasena.matches(".*\\p{L}.*")
			|| !contrasena.matches(".*\\d.*")) {
			throw ErroresIdentidad.contrasenaDebil();
		}
	}
}
