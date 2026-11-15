package co.surco.fincas.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un terreno (lote) del productor: su contorno, dónde queda y si tiene riego.
 *
 * @param municipio código DIVIPOLA de 5 dígitos (lo valida el caso de uso con el módulo territorio)
 */
public record Terreno(UUID id, UUID usuarioId, String nombre, String municipio, boolean riego, Poligono poligono,
	Instant creadoEn) {

	public static final int LARGO_MAXIMO_NOMBRE = 60;

	public Terreno {
		Objects.requireNonNull(id);
		Objects.requireNonNull(usuarioId);
		Objects.requireNonNull(poligono);
		Objects.requireNonNull(creadoEn);
		nombre = nombre == null ? "" : nombre.strip().replaceAll("\\s+", " ");
		if (nombre.isEmpty() || nombre.length() > LARGO_MAXIMO_NOMBRE) {
			throw ErroresFincas.nombreInvalido();
		}
		if (municipio == null || !municipio.matches("\\d{5}")) {
			throw ErroresFincas.municipioInexistente();
		}
	}

	public static Terreno nuevo(UUID usuarioId, String nombre, String municipio, boolean riego, Poligono poligono,
		Instant ahora) {
		return new Terreno(UUID.randomUUID(), usuarioId, nombre, municipio, riego, poligono, ahora);
	}

	public String departamento() {
		return municipio.substring(0, 2);
	}
}
