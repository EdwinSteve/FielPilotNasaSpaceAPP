package co.surco.recomendacion.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Tabla recomendacion.recomendaciones (migración V5). Solo la infraestructura conoce esta clase. */
@Entity
@Table(schema = "recomendacion", name = "recomendaciones")
class RecomendacionJpa {

	@Id
	UUID id;
	@Column(name = "usuario_id", nullable = false)
	UUID usuarioId;
	@Column(name = "terreno_id", nullable = false)
	UUID terrenoId;
	@Column(name = "creada_en", nullable = false)
	Instant creadaEn;
	@Column(name = "siembra_pedida")
	LocalDate siembraPedida;
	@Column(nullable = false)
	String motor;
	@Column(nullable = false)
	String modelo;
	@Column(name = "plan_json", nullable = false)
	String planJson;

	protected RecomendacionJpa() {
	}
}
