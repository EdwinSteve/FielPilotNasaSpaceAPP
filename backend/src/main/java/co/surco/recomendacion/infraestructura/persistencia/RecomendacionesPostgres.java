package co.surco.recomendacion.infraestructura.persistencia;

import co.surco.recomendacion.dominio.PlanCiclo;
import co.surco.recomendacion.dominio.Puertos.RepositorioRecomendaciones;
import co.surco.recomendacion.dominio.Recomendacion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Guarda la respuesta completa del planificador como JSON (con el mismo mapeador snake_case de la API):
 * se lee igual que llegó, aunque el planificador agregue campos nuevos.
 */
@Repository
class RecomendacionesPostgres implements RepositorioRecomendaciones {

	private final RecomendacionesJpa jpa;
	private final ObjectMapper json;

	RecomendacionesPostgres(RecomendacionesJpa jpa, ObjectMapper json) {
		this.jpa = jpa;
		this.json = json;
	}

	@Override
	@Transactional
	public void guardar(Recomendacion r) {
		var e = new RecomendacionJpa();
		e.id = r.id();
		e.usuarioId = r.usuarioId();
		e.terrenoId = r.terrenoId();
		e.creadaEn = r.creadaEn();
		e.siembraPedida = r.siembraPedida();
		e.motor = r.plan().motor() == null ? "red" : r.plan().motor().id();
		e.modelo = r.plan().modelo() == null ? "" : r.plan().modelo();
		e.planJson = json.writeValueAsString(r.plan());
		jpa.saveAndFlush(e);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Recomendacion> porId(UUID usuarioId, UUID terrenoId, UUID id) {
		return jpa.findByIdAndTerrenoIdAndUsuarioId(id, terrenoId, usuarioId).map(this::aDominio);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Recomendacion> ultima(UUID usuarioId, UUID terrenoId) {
		return jpa.findFirstByTerrenoIdAndUsuarioIdOrderByCreadaEnDesc(terrenoId, usuarioId).map(this::aDominio);
	}

	private Recomendacion aDominio(RecomendacionJpa e) {
		return new Recomendacion(e.id, e.usuarioId, e.terrenoId, e.creadaEn, e.siembraPedida,
			json.readValue(e.planJson, PlanCiclo.class));
	}
}
