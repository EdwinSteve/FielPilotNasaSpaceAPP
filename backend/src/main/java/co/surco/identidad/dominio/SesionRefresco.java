package co.surco.identidad.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un refresh token emitido. En la base solo se guarda su hash SHA-256: si alguien roba la base,
 * no puede usar los tokens.
 *
 * <p>Rotación: cada refresh token sirve UNA vez. Al usarlo se marca como usado y se emite otro de la
 * misma {@code familia}. Si un token ya usado vuelve a llegar, alguien lo copió: se revoca toda la familia
 * y tanto el ladrón como el dueño tienen que volver a iniciar sesión.
 */
public class SesionRefresco {

	private final UUID id;
	private final UUID usuarioId;
	private final UUID familia;
	private final String hashToken;
	private final Instant creadaEn;
	private final Instant expiraEn;
	private Instant usadaEn;
	private Instant revocadaEn;

	public SesionRefresco(UUID id, UUID usuarioId, UUID familia, String hashToken, Instant creadaEn,
		Instant expiraEn, Instant usadaEn, Instant revocadaEn) {
		this.id = Objects.requireNonNull(id);
		this.usuarioId = Objects.requireNonNull(usuarioId);
		this.familia = Objects.requireNonNull(familia);
		this.hashToken = Objects.requireNonNull(hashToken);
		this.creadaEn = Objects.requireNonNull(creadaEn);
		this.expiraEn = Objects.requireNonNull(expiraEn);
		this.usadaEn = usadaEn;
		this.revocadaEn = revocadaEn;
	}

	public static SesionRefresco nueva(UUID usuarioId, UUID familia, String hashToken, Instant ahora, Instant expiraEn) {
		return new SesionRefresco(UUID.randomUUID(), usuarioId, familia, hashToken, ahora, expiraEn, null, null);
	}

	/** Ya se usó o se revocó: si llega otra vez, es una reutilización sospechosa. */
	public boolean fueConsumida() {
		return usadaEn != null || revocadaEn != null;
	}

	public boolean estaVencida(Instant ahora) {
		return !ahora.isBefore(expiraEn);
	}

	public void marcarUsada(Instant ahora) {
		this.usadaEn = ahora;
	}

	public UUID id() {
		return id;
	}

	public UUID usuarioId() {
		return usuarioId;
	}

	public UUID familia() {
		return familia;
	}

	public String hashToken() {
		return hashToken;
	}

	public Instant creadaEn() {
		return creadaEn;
	}

	public Instant expiraEn() {
		return expiraEn;
	}

	public Instant usadaEn() {
		return usadaEn;
	}

	public Instant revocadaEn() {
		return revocadaEn;
	}
}
