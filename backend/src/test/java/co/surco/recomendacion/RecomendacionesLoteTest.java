package co.surco.recomendacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.surco.compartido.error.ErrorDominio;
import co.surco.compartido.error.TipoError;
import co.surco.recomendacion.ConsultaRecomendaciones.CultivoEvaluado;
import co.surco.recomendacion.ConsultaRecomendaciones.LluviaMensual;
import co.surco.recomendacion.aplicacion.CalcularRecomendacion;
import co.surco.recomendacion.dominio.Lluvias;
import co.surco.recomendacion.dominio.PlanCiclo;
import co.surco.recomendacion.dominio.Puertos.RepositorioRecomendaciones;
import co.surco.recomendacion.dominio.Puertos.TerrenoUbicado;
import co.surco.recomendacion.dominio.Puntaje;
import co.surco.recomendacion.dominio.Recomendacion;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Recomendaciones guardadas por lote: contrato con el planificador (Bf), puntaje, lluvias y caso de uso. */
class RecomendacionesLoteTest {

	static PlanCiclo planNeiva() {
		return PlanesDePrueba.neiva();
	}

	static PlanCiclo.Cultivo cultivo(String id, double probExito, double rendP50, double rendConRiego, boolean apto) {
		return new PlanCiclo.Cultivo(id, id, 120, LocalDate.of(2027, 3, 1), 0, rendP50, 1, probExito, 50,
			rendConRiego, apto, apto ? null : "altitud", "Poaceae", false, null);
	}

	/** Contrato con ml/planificador: una respuesta real (motor Bf) trae lo que usan recomendación y rotación. */
	@Test
	void leeLaRespuestaBfConFamiliasEHistorico() throws Exception {
		PlanCiclo p = planNeiva();
		assertThat(p.motor().id()).isEqualTo("bf");
		assertThat(p.lote().elevacionM()).isEqualTo(446.0);
		PlanCiclo.Cultivo frijol = p.opciones().getFirst().cultivos().stream()
			.filter(c -> c.cultivo().equals("frijol")).findFirst().orElseThrow();
		assertThat(frijol.familia()).isEqualTo("Fabaceae");
		assertThat(frijol.fija()).isTrue();
		assertThat(frijol.fijacionNitrogenoKgHa()).isEqualTo(60);
		assertThat(p.historico().fechas()).hasSize(35);
		assertThat(p.historico().cultivos()).containsKeys("maiz", "frijol", "sorgo");
		assertThat(p.historico().cultivos().get("maiz").probExito()).hasSize(35);
		// los cultivos que puntúa el modelo estacional: nunca perennes como café o cacao
		assertThat(p.opciones().getFirst().cultivos()).extracting(PlanCiclo.Cultivo::cultivo)
			.hasSize(23).doesNotContain("cafe", "cacao");
	}

	@Nested
	class PuntajeDelRanking {

		@Test
		void enSecanoPromediaProbabilidadYRendimiento() {
			assertThat(Puntaje.de(cultivo("maiz", 0.8, 0.6, 0.9, true), false)).isEqualTo(0.7);
		}

		@Test
		void conRiegoUsaElRendimientoRegado() {
			assertThat(Puntaje.de(cultivo("maiz", 0.2, 0.3, 0.92, true), true)).isEqualTo(0.92);
		}

		@Test
		void seRecortaEntreCeroYUno() {
			assertThat(Puntaje.de(cultivo("maiz", 0, -0.2, 0, true), false)).isZero();
		}

		@Test
		void losAptosVanPrimeroOrdenadosPorPuntaje() throws Exception {
			List<CultivoEvaluado> r = Puntaje.ranking(planNeiva());
			int primerNoApto = (int) r.stream().takeWhile(CultivoEvaluado::apto).count();
			assertThat(r.subList(primerNoApto, r.size())).allSatisfy(c -> {
				assertThat(c.apto()).isFalse();
				assertThat(c.razonNoApto()).isNotBlank();
			});
			assertThat(r.subList(0, primerNoApto)).isSortedAccordingTo(
				Comparator.comparingDouble(CultivoEvaluado::puntaje).reversed());
			assertThat(r).allSatisfy(c -> assertThat(c.puntaje()).isBetween(0.0, 1.0));
		}
	}

	@Nested
	class LluviaPorMes {

		static PlanCiclo.Bloque bloque(int p50, int normal, int pct) {
			return new PlanCiclo.Bloque(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 12, 15), p50 / 2, p50,
				p50 * 2, normal, pct, 30, 20, 30);
		}

		@Test
		void clasificaPorAnomaliaDeVeintePorCiento() throws Exception {
			var lluvias = Lluvias.de(planNeiva());
			assertThat(lluvias.meses()).isNotEmpty();
			assertThat(lluvias.fuente()).startsWith("NASA POWER · años parecidos según El Niño (ONI ");
			LluviaMensual primero = lluvias.meses().getFirst();
			assertThat(primero.mes()).isEqualTo("Diciembre"); // la mitad del bloque 16 nov - 15 dic
			assertThat(primero.nivel()).isEqualTo("normal"); // -9 % frente a lo normal
		}

		@Test
		void secoYLluviosoTraenQueHacer() {
			var plan = new PlanCiclo(null, null, new PlanCiclo.Pronostico("analogos", 1.2, 0.3, "nino",
				List.of(bloque(80, 160, -50), bloque(260, 160, 62), bloque(150, 160, -6))), null, List.of(), null, "m",
				null);
			assertThat(Lluvias.de(plan).meses()).extracting(LluviaMensual::nivel)
				.containsExactly("seco", "lluvioso", "normal");
			assertThat(Lluvias.de(plan).meses().getFirst().nota()).contains("80 mm frente a 160 mm");
			assertThat(Lluvias.de(plan).meses().get(2).nota()).isNull();
		}
	}

	@Nested
	class CasoDeUso {

		final UUID ana = UUID.randomUUID();
		final TerrenoUbicado terreno = new TerrenoUbicado(UUID.randomUUID(), "El Mirador", "41001", 2.9275,
			-75.2875, 4.2, false);
		final List<Recomendacion> guardadas = new ArrayList<>();
		final List<LocalDate> siembrasPedidas = new ArrayList<>();
		final Clock reloj = Clock.fixed(Instant.parse("2026-11-15T15:00:00Z"), ZoneOffset.UTC);

		CalcularRecomendacion caso() throws Exception {
			PlanCiclo plan = planNeiva();
			RepositorioRecomendaciones repo = new RepositorioRecomendaciones() {
				@Override
				public void guardar(Recomendacion r) {
					guardadas.add(r);
				}

				@Override
				public Optional<Recomendacion> porId(UUID u, UUID t, UUID id) {
					return guardadas.stream().filter(r -> r.id().equals(id) && r.terrenoId().equals(t)
						&& r.usuarioId().equals(u)).findFirst();
				}

				@Override
				public Optional<Recomendacion> ultima(UUID u, UUID t) {
					return guardadas.reversed().stream().filter(r -> r.terrenoId().equals(t) && r.usuarioId().equals(u))
						.findFirst();
				}
			};
			return new CalcularRecomendacion((u, id) -> u.equals(ana) && id.equals(terreno.id())
				? Optional.of(terreno) : Optional.empty(), (lat, lon, riego, siembra) -> {
					siembrasPedidas.add(siembra);
					return plan;
				}, repo, reloj);
		}

		@Test
		void calculaGuardaYLuegoLeeLaUltima() throws Exception {
			var caso = caso();
			var c = caso.ejecutar(ana, terreno.id(), null);
			assertThat(guardadas).singleElement().isSameAs(c.recomendacion());
			assertThat(c.recomendacion().creadaEn()).isEqualTo(reloj.instant());
			assertThat(caso.ultima(ana, terreno.id()).recomendacion().id()).isEqualTo(c.recomendacion().id());
			assertThat(caso.obtener(ana, terreno.id(), c.recomendacion().id()).terreno().nombre())
				.isEqualTo("El Mirador");
		}

		@Test
		void laApiPublicaDaRankingHistoricoYLluvias() throws Exception {
			var r = caso().calcular(ana, terreno.id());
			assertThat(r.motor()).isEqualTo("bf");
			assertThat(r.nombreTerreno()).isEqualTo("El Mirador");
			assertThat(r.cultivos()).hasSize(23);
			assertThat(r.historico().fechas()).hasSize(35);
			assertThat(r.historico().probExito().get("maiz")).hasSize(35);
			assertThat(r.lluvias().meses()).isNotEmpty();
		}

		@Test
		void validaLaFechaDeSiembra() throws Exception {
			var caso = caso();
			caso.ejecutar(ana, terreno.id(), LocalDate.of(2026, 12, 1));
			assertThat(siembrasPedidas).containsExactly(LocalDate.of(2026, 12, 1));
			assertThatThrownBy(() -> caso.ejecutar(ana, terreno.id(), LocalDate.of(2026, 11, 1)))
				.isInstanceOfSatisfying(ErrorDominio.class,
					e -> assertThat(e.codigo()).isEqualTo("RECOMENDACION_SIEMBRA_INVALIDA"));
			assertThatThrownBy(() -> caso.ejecutar(ana, terreno.id(), LocalDate.of(2027, 3, 1)))
				.isInstanceOf(ErrorDominio.class);
		}

		@Test
		void unTerrenoAjenoONingunaRecomendacionResponden404() throws Exception {
			var caso = caso();
			assertThatThrownBy(() -> caso.ejecutar(UUID.randomUUID(), terreno.id(), null))
				.isInstanceOfSatisfying(ErrorDominio.class, e -> assertThat(e.tipo()).isEqualTo(TipoError.NO_ENCONTRADO));
			assertThatThrownBy(() -> caso.ultima(ana, terreno.id()))
				.isInstanceOfSatisfying(ErrorDominio.class,
					e -> assertThat(e.codigo()).isEqualTo("RECOMENDACION_NO_ENCONTRADA"));
			assertThat(siembrasPedidas).isEmpty();
		}
	}
}
