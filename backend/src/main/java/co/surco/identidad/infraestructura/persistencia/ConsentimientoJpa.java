package co.surco.identidad.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Tabla identidad.consentimientos: prueba de que el usuario aceptó una versión de la política (Ley 1581). */
@Entity
@Table(schema = "identidad", name = "consentimientos")
class ConsentimientoJpa {

	@Id
	private UUID id;
	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;
	@Column(name = "version_politica", nullable = false)
	private String versionPolitica;
	@Column(name = "aceptado_en", nullable = false)
	private Instant aceptadoEn;

	protected ConsentimientoJpa() {
	}

	ConsentimientoJpa(UUID usuarioId, String versionPolitica, Instant aceptadoEn) {
		this.id = UUID.randomUUID();
		this.usuarioId = usuarioId;
		this.versionPolitica = versionPolitica;
		this.aceptadoEn = aceptadoEn;
	}
}
