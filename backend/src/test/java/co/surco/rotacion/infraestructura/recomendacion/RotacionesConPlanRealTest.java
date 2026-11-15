package co.surco.rotacion.infraestructura.recomendacion;

import static org.assertj.core.api.Assertions.assertThat;

import co.surco.recomendacion.PlanesDePrueba;
import co.surco.recomendacion.aplicacion.CalcularRecomendacion;
import co.surco.recomendacion.aplicacion.CalcularRecomendacion.Calculada;
import co.surco.recomendacion.dominio.Puertos.TerrenoUbicado;
import co.surco.recomendacion.dominio.Recomendacion;
import co.surco.rotacion.dominio.Candidatos;
import co.surco.rotacion.dominio.GeneradorRotaciones;
import co.surco.rotacion.dominio.Rotacion;
import co.surco.rotacion.dominio.Rotacion.Temporada;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** De la respuesta real del planificador (Neiva, Bf) a las 3 rotaciones, por la API pública de recomendacion. */
class RotacionesConPlanRealTest {

	static Candidatos neiva() {
		var terreno = new TerrenoUbicado(UUID.randomUUID(), "El Mirador", "41001", 2.9275, -75.2875, 4.2, false);
		var r = new Recomendacion(UUID.randomUUID(), UUID.randomUUID(), terreno.id(), Instant.now(), null,
			PlanesDePrueba.neiva());
		return RecomendacionesDelModelo.candidatos(CalcularRecomendacion.publica(new Calculada(terreno, r)));
	}

	@Test
	void tresRotacionesValidasEnNeiva() {
		Candidatos c = neiva();
		assertThat(c.primera()).hasSize(23);
		assertThat(c.ranking()).extracting(Candidatos.CultivoRanking::id).doesNotContain("cafe", "cacao");
		List<Rotacion> r = GeneradorRotaciones.mejores(c);
		assertThat(r).hasSize(3);
		assertThat(r).allSatisfy(rot -> {
			assertThat(rot.temporadas()).hasSizeBetween(2, 3);
			assertThat(rot.temporadas().getFirst().fuente()).isEqualTo("pronostico");
			for (int i = 1; i < rot.temporadas().size(); i++) {
				Temporada t = rot.temporadas().get(i);
				assertThat(t.familia()).isNotEqualTo(rot.temporadas().get(i - 1).familia());
				assertThat(t.siembra()).isAfter(rot.temporadas().get(i - 1).cosecha());
			}
			assertThat(rot.grado()).isIn("A+", "A", "B", "C", "D");
			assertThat(rot.frecuenciaExito()).isBetween(0, 10);
			assertThat(rot.aguaAhorradaM3()).isNotNegative();
		});
		// en Neiva (446 m) no entran cultivos de clima frío como la papa
		assertThat(r).flatExtracting(Rotacion::temporadas).extracting(Temporada::cultivo).doesNotContain("papa");
	}
}
