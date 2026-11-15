package co.surco.recomendacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.surco.compartido.error.ErrorDominio;
import co.surco.recomendacion.aplicacion.RecomendarCiclo;
import co.surco.recomendacion.dominio.PlanCiclo;
import co.surco.recomendacion.dominio.Puertos.TerrenoUbicado;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

class RecomendacionTest {

	/** Igual que la API: spring.jackson.property-naming-strategy = SNAKE_CASE. */
	static final JsonMapper JSON = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
		.build();

	static PlanCiclo planDeMuestra() throws Exception {
		try (InputStream in = RecomendacionTest.class.getResourceAsStream("/planificador/plan-manizales.json")) {
			return JSON.readValue(in, PlanCiclo.class);
		}
	}

	/** Contrato con ml/planificador: una respuesta real de servidor.py se lee completa. */
	@Test
	void leeLaRespuestaRealDelPlanificador() throws Exception {
		PlanCiclo p = planDeMuestra();
		assertThat(p.lote().elevacionM()).isEqualTo(2116.0);
		assertThat(p.pronostico().faseEnso()).isEqualTo("nino");
		assertThat(p.pronostico().oniTendencia()).isPositive();
		assertThat(p.pronostico().panorama()).isNotEmpty();
		PlanCiclo.Bloque b = p.pronostico().panorama().getFirst();
		assertThat(b.lluviaMmP10()).isLessThanOrEqualTo(b.lluviaMmP50());
		assertThat(b.lluviaNormalMm()).isPositive();
		assertThat(p.mejor().siembra()).isEqualTo(LocalDate.of(2026, 12, 6));
		assertThat(p.mejor().cultivo().nombre()).isEqualTo("Maíz");
		assertThat(p.mejor().cultivo().cicloDias()).isEqualTo(140);
		assertThat(p.opciones()).allSatisfy(o -> assertThat(o.cultivos()).isNotEmpty());
		PlanCiclo.Cultivo noApto = p.opciones().getFirst().cultivos().getLast();
		assertThat(noApto.apto()).isFalse();
		assertThat(noApto.razonNoApto()).contains("altitud");
	}

	@Test
	void recomiendaConElCentroideYElRiegoDelTerreno() throws Exception {
		UUID ana = UUID.randomUUID();
		var terreno = new TerrenoUbicado(UUID.randomUUID(), "Lote de arriba", "17001", 5.07, -75.52, 4.9, true);
		List<double[]> llamadas = new ArrayList<>();
		PlanCiclo plan = planDeMuestra();
		var caso = new RecomendarCiclo((u, id) -> u.equals(ana) && id.equals(terreno.id()) ? Optional.of(terreno)
			: Optional.empty(), (lat, lon, riego, siembra) -> {
				llamadas.add(new double[] {lat, lon, riego ? 1 : 0});
				return plan;
			});

		var r = caso.para(ana, terreno.id());
		assertThat(r.terreno().nombre()).isEqualTo("Lote de arriba");
		assertThat(r.plan()).isSameAs(plan);
		assertThat(llamadas).singleElement().satisfies(l -> assertThat(l).containsExactly(5.07, -75.52, 1));

		assertThatThrownBy(() -> caso.para(UUID.randomUUID(), terreno.id()))
			.isInstanceOfSatisfying(ErrorDominio.class,
				e -> assertThat(e.codigo()).isEqualTo("RECOMENDACION_TERRENO_NO_ENCONTRADO"));
		assertThat(llamadas).hasSize(1); // un terreno ajeno ni siquiera llega al modelo
	}
}
