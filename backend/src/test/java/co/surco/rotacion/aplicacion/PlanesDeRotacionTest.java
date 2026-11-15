package co.surco.rotacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.surco.compartido.error.ErrorDominio;
import co.surco.rotacion.dominio.PlanRotaciones;
import co.surco.rotacion.dominio.Puertos.RepositorioPlanes;
import co.surco.rotacion.dominio.Puertos.RepositorioSelecciones;
import co.surco.rotacion.dominio.Seleccion;
import co.surco.rotacion.infraestructura.recomendacion.CandidatosDePrueba;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Calcular, leer y elegir, con repositorios en memoria. */
class PlanesDeRotacionTest {

	final UUID carlos = UUID.randomUUID();
	final UUID lote = UUID.randomUUID();
	final List<PlanRotaciones> planes = new ArrayList<>();
	final Map<UUID, Seleccion> selecciones = new HashMap<>();
	final Clock reloj = Clock.fixed(Instant.parse("2026-11-15T15:00:00Z"), ZoneOffset.UTC);

	final PlanesDeRotacion casos = new PlanesDeRotacion((u, t) -> CandidatosDePrueba.neiva(), new RepositorioPlanes() {
		@Override
		public void guardar(PlanRotaciones p) {
			planes.add(p);
		}

		@Override
		public Optional<PlanRotaciones> ultimo(UUID u, UUID t) {
			return planes.reversed().stream().filter(p -> p.usuarioId().equals(u) && p.terrenoId().equals(t)).findFirst();
		}

		@Override
		public Optional<PlanRotaciones> porId(UUID u, UUID t, UUID id) {
			return planes.stream().filter(p -> p.id().equals(id) && p.usuarioId().equals(u) && p.terrenoId().equals(t))
				.findFirst();
		}
	}, new RepositorioSelecciones() {
		@Override
		public void guardar(Seleccion s) {
			selecciones.put(s.terrenoId(), s);
		}

		@Override
		public Optional<Seleccion> de(UUID u, UUID t) {
			return Optional.ofNullable(selecciones.get(t)).filter(s -> s.usuarioId().equals(u));
		}
	}, reloj);

	@Test
	void calculaGuardaYLeeElUltimoPlan() {
		var calculado = casos.calcular(carlos, lote);
		assertThat(calculado.plan().rotaciones()).hasSize(3);
		assertThat(calculado.plan().ranking()).hasSize(23);
		assertThat(calculado.plan().lluvias().meses()).isNotEmpty();
		assertThat(calculado.seleccion()).isEmpty();
		assertThat(casos.ultimo(carlos, lote).plan().id()).isEqualTo(calculado.plan().id());
	}

	@Test
	void eligeUnCicloYLoReemplazaAlElegirOtro() {
		var plan = casos.calcular(carlos, lote).plan();
		String primera = plan.rotaciones().get(0).id();
		String segunda = plan.rotaciones().get(1).id();

		Seleccion s = casos.elegir(carlos, lote, plan.id(), primera);
		assertThat(s.secuencia()).isEqualTo(plan.rotaciones().get(0).secuencia());
		assertThat(s.elegidaEn()).isEqualTo(reloj.instant());
		casos.elegir(carlos, lote, plan.id(), segunda);
		assertThat(casos.seleccion(carlos, lote).rotacionId()).isEqualTo(segunda);
		assertThat(casos.ultimo(carlos, lote).seleccion()).map(Seleccion::rotacionId).contains(segunda);
	}

	@Test
	void erroresConCodigo() {
		assertThatThrownBy(() -> casos.ultimo(carlos, lote)).isInstanceOfSatisfying(ErrorDominio.class,
			e -> assertThat(e.codigo()).isEqualTo("ROTACION_SIN_CALCULAR"));
		assertThatThrownBy(() -> casos.seleccion(carlos, lote)).isInstanceOfSatisfying(ErrorDominio.class,
			e -> assertThat(e.codigo()).isEqualTo("ROTACION_SIN_SELECCION"));
		var plan = casos.calcular(carlos, lote).plan();
		assertThatThrownBy(() -> casos.elegir(carlos, lote, plan.id(), "papa-papa")).isInstanceOfSatisfying(
			ErrorDominio.class, e -> assertThat(e.codigo()).isEqualTo("ROTACION_NO_ENCONTRADA"));
		// el plan de otro productor no se puede elegir
		assertThatThrownBy(() -> casos.elegir(UUID.randomUUID(), lote, plan.id(), plan.rotaciones().getFirst().id()))
			.isInstanceOfSatisfying(ErrorDominio.class, e -> assertThat(e.codigo()).isEqualTo("ROTACION_SIN_CALCULAR"));
	}
}
