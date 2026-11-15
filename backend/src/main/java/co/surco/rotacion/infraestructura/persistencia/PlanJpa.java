package co.surco.rotacion.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Tabla rotacion.planes (migración V6). El plan completo va como JSON. */
@Entity
@Table(schema = "rotacion", name = "planes")
class PlanJpa {

	@Id
	UUID id;
	@Column(name = "usuario_id", nullable = false)
	UUID usuarioId;
	@Column(name = "terreno_id", nullable = false)
	UUID terrenoId;
	@Column(name = "recomendacion_id", nullable = false)
	UUID recomendacionId;
	@Column(name = "creado_en", nullable = false)
	Instant creadoEn;
	@Column(name = "contenido_json", nullable = false)
	String contenidoJson;

	protected PlanJpa() {
	}
}
