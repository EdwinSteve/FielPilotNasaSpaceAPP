package co.surco.identidad.infraestructura.persistencia;

import co.surco.identidad.dominio.SesionRefresco;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Tabla identidad.sesiones: un refresh token emitido (solo su hash). */
@Entity
@Table(schema = "identidad", name = "sesiones")
class SesionJpa {

	@Id
	private UUID id;
	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;
	@Column(nullable = false)
	private UUID familia;
	@Column(name = "hash_token", nullable = false, unique = true)
	private String hashToken;
	@Column(name = "creada_en", nullable = false)
	private Instant creadaEn;
	@Column(name = "expira_en", nullable = false)
	private Instant expiraEn;
	@Column(name = "usada_en")
	private Instant usadaEn;
	@Column(name = "revocada_en")
	private Instant revocadaEn;

	protected SesionJpa() {
	}

	static SesionJpa desde(SesionRefresco s) {
		SesionJpa e = new SesionJpa();
		e.id = s.id();
		e.usuarioId = s.usuarioId();
		e.familia = s.familia();
		e.hashToken = s.hashToken();
		e.creadaEn = s.creadaEn();
		e.expiraEn = s.expiraEn();
		e.usadaEn = s.usadaEn();
		e.revocadaEn = s.revocadaEn();
		return e;
	}

	SesionRefresco aDominio() {
		return new SesionRefresco(id, usuarioId, familia, hashToken, creadaEn, expiraEn, usadaEn, revocadaEn);
	}
}
