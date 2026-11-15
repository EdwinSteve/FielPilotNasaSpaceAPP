package co.surco.rotacion.infraestructura.persistencia;

import co.surco.rotacion.dominio.PlanRotaciones;
import co.surco.rotacion.dominio.Puertos.RepositorioPlanes;
import co.surco.rotacion.dominio.Puertos.RepositorioSelecciones;
import co.surco.rotacion.dominio.Seleccion;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Adaptadores PostgreSQL de los puertos de rotacion (esquema "rotacion"). */
final class RotacionesPostgres {

	static final String SEPARADOR = " → ";

	private RotacionesPostgres() {
	}

	@Repository
	static class Planes implements RepositorioPlanes {

		private final PlanesJpa jpa;
		private final ObjectMapper json;

		Planes(PlanesJpa jpa, ObjectMapper json) {
			this.jpa = jpa;
			this.json = json;
		}

		@Override
		@Transactional
		public void guardar(PlanRotaciones p) {
			var e = new PlanJpa();
			e.id = p.id();
			e.usuarioId = p.usuarioId();
			e.terrenoId = p.terrenoId();
			e.recomendacionId = p.recomendacionId();
			e.creadoEn = p.creadoEn();
			e.contenidoJson = json.writeValueAsString(p);
			jpa.saveAndFlush(e);
		}

		@Override
		@Transactional(readOnly = true)
		public Optional<PlanRotaciones> ultimo(UUID usuarioId, UUID terrenoId) {
			return jpa.findFirstByTerrenoIdAndUsuarioIdOrderByCreadoEnDesc(terrenoId, usuarioId).map(this::leer);
		}

		@Override
		@Transactional(readOnly = true)
		public Optional<PlanRotaciones> porId(UUID usuarioId, UUID terrenoId, UUID planId) {
			return jpa.findByIdAndTerrenoIdAndUsuarioId(planId, terrenoId, usuarioId).map(this::leer);
		}

		private PlanRotaciones leer(PlanJpa e) {
			return json.readValue(e.contenidoJson, PlanRotaciones.class);
		}
	}

	@Repository
	static class Selecciones implements RepositorioSelecciones {

		private final SeleccionesJpa jpa;

		Selecciones(SeleccionesJpa jpa) {
			this.jpa = jpa;
		}

		@Override
		@Transactional
		public void guardar(Seleccion s) {
			var e = jpa.findById(s.terrenoId()).orElseGet(SeleccionJpa::new);
			e.terrenoId = s.terrenoId();
			e.usuarioId = s.usuarioId();
			e.planId = s.planId();
			e.rotacionId = s.rotacionId();
			e.secuencia = String.join(SEPARADOR, s.secuencia());
			e.elegidaEn = s.elegidaEn();
			jpa.saveAndFlush(e);
		}

		@Override
		@Transactional(readOnly = true)
		public Optional<Seleccion> de(UUID usuarioId, UUID terrenoId) {
			return jpa.findByTerrenoIdAndUsuarioId(terrenoId, usuarioId).map(e -> new Seleccion(e.terrenoId,
				e.usuarioId, e.planId, e.rotacionId, Arrays.asList(e.secuencia.split(SEPARADOR)), e.elegidaEn));
		}
	}
}
