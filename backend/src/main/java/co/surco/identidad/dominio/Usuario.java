package co.surco.identidad.dominio;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Cuenta de un productor. Guarda el hash de la contraseña (nunca la contraseña) y el estado
 * de bloqueo por intentos fallidos.
 */
public class Usuario {

	private final UUID id;
	private final String nombreCompleto;
	private final Celular celular;
	private final Correo correo;
	private final String contrasenaHash;
	private final String municipioResidencia;
	private final Rol rol;
	private final Instant creadoEn;
	private int intentosFallidos;
	private Instant bloqueadoHasta;

	public Usuario(UUID id, String nombreCompleto, Celular celular, Correo correo, String contrasenaHash,
		String municipioResidencia, Rol rol, Instant creadoEn, int intentosFallidos, Instant bloqueadoHasta) {
		this.id = Objects.requireNonNull(id);
		this.nombreCompleto = Objects.requireNonNull(nombreCompleto);
		this.celular = Objects.requireNonNull(celular);
		this.correo = correo;
		this.contrasenaHash = Objects.requireNonNull(contrasenaHash);
		this.municipioResidencia = Objects.requireNonNull(municipioResidencia);
		this.rol = Objects.requireNonNull(rol);
		this.creadoEn = Objects.requireNonNull(creadoEn);
		this.intentosFallidos = intentosFallidos;
		this.bloqueadoHasta = bloqueadoHasta;
	}

	/** Cuenta nueva de productor. */
	public static Usuario nuevo(String nombreCompleto, Celular celular, Correo correo, String contrasenaHash,
		String municipioResidencia, Instant ahora) {
		String nombre = nombreCompleto == null ? "" : nombreCompleto.trim().replaceAll("\\s+", " ");
		if (nombre.length() < 3 || nombre.length() > 120) {
			throw ErroresIdentidad.nombreInvalido();
		}
		return new Usuario(UUID.randomUUID(), nombre, celular, correo, contrasenaHash, municipioResidencia,
			Rol.PRODUCTOR, ahora, 0, null);
	}

	public boolean estaBloqueado(Instant ahora) {
		return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
	}

	/** Suma un intento fallido; al llegar al máximo bloquea la cuenta y reinicia el conteo. */
	public void registrarIntentoFallido(Instant ahora, int maximoIntentos, Duration duracionBloqueo) {
		intentosFallidos++;
		if (intentosFallidos >= maximoIntentos) {
			bloqueadoHasta = ahora.plus(duracionBloqueo);
			intentosFallidos = 0;
		}
	}

	public void registrarIngresoExitoso() {
		intentosFallidos = 0;
		bloqueadoHasta = null;
	}

	public UUID id() {
		return id;
	}

	public String nombreCompleto() {
		return nombreCompleto;
	}

	public Celular celular() {
		return celular;
	}

	public Correo correo() {
		return correo;
	}

	public String contrasenaHash() {
		return contrasenaHash;
	}

	public String municipioResidencia() {
		return municipioResidencia;
	}

	public Rol rol() {
		return rol;
	}

	public Instant creadoEn() {
		return creadoEn;
	}

	public int intentosFallidos() {
		return intentosFallidos;
	}

	public Instant bloqueadoHasta() {
		return bloqueadoHasta;
	}
}
