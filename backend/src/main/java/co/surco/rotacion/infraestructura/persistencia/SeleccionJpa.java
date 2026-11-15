package co.surco.rotacion.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Tabla rotacion.selecciones (migración V6): una fila por lote, el ciclo activo. */
@Entity
@Table(schema = "rotacion", name = "selecciones")
class SeleccionJpa {

	@Id
	@Column(name = "terreno_id")
	UUID terrenoId;
	@Column(name = "usuario_id", nullable = false)
	UUID usuarioId;
	@Column(name = "plan_id", nullable = false)
	UUID planId;
	@Column(name = "rotacion_id", nullable = false)
	String rotacionId;
	/** Nombres de los cultivos en orden, separados por " → " (lo que se muestra). */
	@Column(nullable = false)
	String secuencia;
	@Column(name = "elegida_en", nullable = false)
	Instant elegidaEn;

	protected SeleccionJpa() {
	}
}
