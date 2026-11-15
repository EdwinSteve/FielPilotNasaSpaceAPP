package co.surco.rotacion.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import co.surco.rotacion.dominio.Candidatos.Cultivo;
import co.surco.rotacion.dominio.Rotacion.Temporada;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** El algoritmo v1 de rotaciones con datos pequeños hechos a mano. */
class GeneradorRotacionesTest {

	static final LocalDate HOY = LocalDate.of(2026, 11, 15);
	static final List<LocalDate> FECHAS = new ArrayList<>();

	static {
		for (int k = 0; k < 35; k++) {
			FECHAS.add(HOY.plusDays(30 + 15L * k));
		}
	}

	static Cultivo cultivo(String id, String familia, double puntaje, double prob, int ciclo) {
		return new Cultivo(id, id.substring(0, 1).toUpperCase() + id.substring(1), familia, "cereal", false, null, ciclo,
			HOY.plusDays(1), HOY.plusDays(1 + ciclo), puntaje, prob, 100, true);
	}

	static Cultivo leguminosa(String id, double puntaje, double prob, int ciclo, int kg) {
		return new Cultivo(id, id.substring(0, 1).toUpperCase() + id.substring(1), "Fabaceae", "leguminosa", true, kg, ciclo,
			HOY.plusDays(1), HOY.plusDays(1 + ciclo), puntaje, prob, 50, true);
	}

	/** Cada cultivo con la misma P(éxito) histórica en todas las fechas, y riego por cultivo. */
	static Candidatos candidatos(List<Cultivo> cultivos, Map<String, Double> probHistorica,
		Map<String, Double> riegoHistorico) {
		Map<String, List<Double>> prob = new HashMap<>();
		Map<String, List<Double>> riego = new HashMap<>();
		cultivos.forEach(c -> {
			prob.put(c.id(), FECHAS.stream().map(f -> probHistorica.getOrDefault(c.id(), 0.5)).toList());
			riego.put(c.id(), FECHAS.stream().map(f -> riegoHistorico.getOrDefault(c.id(), 100.0)).toList());
		});
		return new Candidatos(UUID.randomUUID(), "El Mirador", 4.2, false, "bf", cultivos,
			new Candidatos.Historico("NASA POWER 1981-2025", FECHAS, prob, riego), List.of(),
			new Candidatos.Lluvias("NASA POWER", List.of()));
	}

	@Test
	void nuncaRepiteLaFamiliaEnTemporadasSeguidas() {
		var c = candidatos(List.of(cultivo("maiz", "Poaceae", 0.9, 0.9, 120), cultivo("sorgo", "Poaceae", 0.8, 0.9, 110),
			cultivo("arroz", "Poaceae", 0.7, 0.9, 120), cultivo("yuca", "Euphorbiaceae", 0.6, 0.6, 240),
			leguminosa("frijol", 0.5, 0.7, 90, 60)), Map.of(), Map.of());
		List<Rotacion> r = GeneradorRotaciones.mejores(c);
		assertThat(r).isNotEmpty().allSatisfy(rot -> {
			List<Temporada> t = rot.temporadas();
			for (int i = 1; i < t.size(); i++) {
				assertThat(t.get(i).familia()).isNotEqualTo(t.get(i - 1).familia());
			}
			assertThat(t.stream().map(Temporada::cultivo).distinct()).hasSameSizeAs(t);
		});
	}

	@Test
	void premiaLaLeguminosaQueDejaNitrogenoAlSiguiente() {
		// Misma P(éxito) para todos: la única diferencia es la leguminosa en medio
		var c = candidatos(List.of(cultivo("maiz", "Poaceae", 0.9, 0.8, 120), leguminosa("frijol", 0.6, 0.8, 90, 60),
			cultivo("yuca", "Euphorbiaceae", 0.5, 0.8, 240)),
			Map.of("maiz", 0.8, "frijol", 0.8, "yuca", 0.8), Map.of());
		Rotacion mejor = GeneradorRotaciones.mejores(c).getFirst();
		assertThat(mejor.temporadas()).extracting(Temporada::cultivo).contains("frijol");
		assertThat(mejor.puntaje()).isEqualTo(0.85);
		assertThat(mejor.probMedia()).isEqualTo(0.8);
		assertThat(mejor.aporteNitrogeno()).contains("Frijol").contains("60 kg de nitrógeno por hectárea");
		assertThat(mejor.grado()).isEqualTo("A+");
		assertThat(mejor.frecuenciaExito()).isEqualTo(8);
	}

	@Test
	void unaLeguminosaAlFinalNoDejaNitrogenoANadie() {
		var seq = List.of(new Temporada("maiz", "Maíz", "Poaceae", HOY, HOY.plusDays(120), 0.8, 100, "pronostico"),
			new Temporada("frijol", "Fríjol", "Fabaceae", HOY.plusDays(135), HOY.plusDays(225), 0.8, 50, "historico"));
		Map<String, Cultivo> porId = Map.of("maiz", cultivo("maiz", "Poaceae", 1, 1, 120), "frijol",
			leguminosa("frijol", 1, 1, 90, 60));
		assertThat(GeneradorRotaciones.aporteNitrogeno(seq, porId)).isNull();
	}

	@Test
	void ordenaPorPuntajeYDevuelveTresDistintas() {
		var c = candidatos(List.of(cultivo("maiz", "Poaceae", 0.9, 0.9, 120), cultivo("yuca", "Euphorbiaceae", 0.8, 0.7, 240),
			leguminosa("soya", 0.7, 0.6, 110, 150), cultivo("papa", "Solanaceae", 0.6, 0.5, 150),
			cultivo("ahuyama", "Cucurbitaceae", 0.5, 0.4, 120)), Map.of("maiz", 0.9, "soya", 0.7), Map.of());
		List<Rotacion> r = GeneradorRotaciones.mejores(c);
		assertThat(r).hasSize(3);
		assertThat(r).extracting(Rotacion::puntaje).isSortedAccordingTo((a, b) -> Double.compare(b, a));
		assertThat(r).extracting(Rotacion::id).doesNotHaveDuplicates();
		// las dos primeras temporadas no se repiten entre opciones
		assertThat(r).extracting(rot -> rot.temporadas().get(0).cultivo() + rot.temporadas().get(1).cultivo())
			.doesNotHaveDuplicates();
	}

	@Test
	void laPrimeraTemporadaUsaElPronosticoYLasSiguientesLaHistoria() {
		var c = candidatos(List.of(cultivo("maiz", "Poaceae", 0.9, 0.95, 120), leguminosa("frijol", 0.5, 0.2, 90, 60)),
			Map.of("frijol", 0.6, "maiz", 0.4), Map.of());
		Rotacion r = GeneradorRotaciones.mejores(c).stream().filter(x -> x.id().startsWith("maiz")).findFirst()
			.orElseThrow();
		Temporada uno = r.temporadas().get(0);
		Temporada dos = r.temporadas().get(1);
		assertThat(uno.fuente()).isEqualTo("pronostico");
		assertThat(uno.probExito()).isEqualTo(0.95);
		assertThat(dos.fuente()).isEqualTo("historico");
		assertThat(dos.probExito()).isEqualTo(0.6);
		assertThat(dos.siembra()).isEqualTo(uno.cosecha().plusDays(GeneradorRotaciones.DESCANSO_DIAS));
	}

	@Test
	void terminaLaSecuenciaCuandoSaleDeLaTablaHistorica() {
		// ciclos de 300 días: la tercera siembra cae más allá de los 540 días de la tabla
		var c = candidatos(List.of(cultivo("yuca", "Euphorbiaceae", 0.9, 0.9, 300), cultivo("name", "Dioscoreaceae", 0.8,
			0.8, 300), cultivo("platano", "Musaceae", 0.7, 0.7, 300)), Map.of(), Map.of());
		assertThat(GeneradorRotaciones.mejores(c)).isNotEmpty().allSatisfy(r -> assertThat(r.temporadas()).hasSize(2));
	}

	@Test
	void soloUsaCultivosAptos() {
		var noApto = new Cultivo("papa", "Papa", "Solanaceae", "tuberculo", false, null, 150, HOY, HOY.plusDays(150), 0.99, 0.99,
			10, false);
		var c = candidatos(List.of(noApto, cultivo("maiz", "Poaceae", 0.5, 0.5, 120), leguminosa("frijol", 0.4, 0.4, 90, 60)),
			Map.of("papa", 1.0), Map.of());
		assertThat(GeneradorRotaciones.mejores(c)).allSatisfy(
			r -> assertThat(r.temporadas()).extracting(Temporada::cultivo).doesNotContain("papa"));
	}

	@Test
	void aguaAhorradaFrenteARepetirElMejorCultivoComercial() {
		// La referencia es el maíz (mayor puntaje): con el pronóstico pide 100 mm y en la historia 300 mm por
		// temporada; el fríjol 50 mm en ambos. Lote de 4,2 ha; 1 mm en 1 ha = 10 m³.
		var c = candidatos(List.of(cultivo("maiz", "Poaceae", 0.9, 0.9, 120), leguminosa("frijol", 0.5, 0.5, 90, 60)),
			Map.of(), Map.of("maiz", 300.0, "frijol", 50.0));
		Rotacion r = GeneradorRotaciones.mejores(c).stream().filter(x -> x.id().equals("maiz-frijol")).findFirst()
			.orElseThrow();
		// referencia maíz-maíz: 100 + 300; secuencia maíz-fríjol: 100 + 50. (400 - 150) × 4,2 × 10 = 10 500
		assertThat(r.aguaAhorradaM3()).isEqualTo(10_500);
		Rotacion alReves = GeneradorRotaciones.mejores(c).stream().filter(x -> x.id().equals("frijol-maiz"))
			.findFirst().orElseThrow();
		// referencia: 100 + 300; secuencia fríjol-maíz: 50 + 300. (400 - 350) × 4,2 × 10 = 2 100
		assertThat(alReves.aguaAhorradaM3()).isEqualTo(2_100);
	}

	@Test
	void elAguaAhorradaNuncaEsNegativa() {
		// el fríjol es la referencia y pide menos riego que el maíz que lo sigue
		var c = candidatos(List.of(leguminosa("frijol", 0.9, 0.9, 90, 60), cultivo("maiz", "Poaceae", 0.5, 0.5, 120)),
			Map.of(), Map.of("maiz", 300.0, "frijol", 50.0));
		assertThat(GeneradorRotaciones.mejores(c)).allSatisfy(r -> assertThat(r.aguaAhorradaM3()).isNotNegative())
			.anySatisfy(r -> assertThat(r.aguaAhorradaM3()).isZero());
	}

	@Test
	void unAbonoVerdeNoEsLaReferenciaDelAgua() {
		// La crotalaria tiene el mayor puntaje pero no se vende: la referencia es el maíz (300 mm por temporada)
		var crotalaria = new Cultivo("crotalaria", "Crotalaria", "Fabaceae", "abono_verde", true, 115, 75, HOY.plusDays(1),
			HOY.plusDays(76), 0.99, 1.0, 20, true);
		var maiz = new Cultivo("maiz", "Maíz", "Poaceae", "cereal", false, null, 120, HOY.plusDays(1), HOY.plusDays(121),
			0.8, 0.9, 300, true);
		var c = candidatos(List.of(crotalaria, maiz), Map.of(), Map.of("maiz", 300.0, "crotalaria", 20.0));
		Rotacion r = GeneradorRotaciones.mejores(c).stream().filter(x -> x.id().equals("crotalaria-maiz")).findFirst()
			.orElseThrow();
		// referencia maíz-maíz: 300 + 300; secuencia crotalaria-maíz: 20 + 300. Ahorro 280 mm × 4,2 ha × 10
		assertThat(r.aguaAhorradaM3()).isEqualTo(11_760);
	}

	@Test
	void gradosPorUmbral() {
		assertThat(List.of(0.9, 0.85, 0.7, 0.69, 0.55, 0.4, 0.39).stream().map(GeneradorRotaciones::grado))
			.containsExactly("A+", "A+", "A", "B", "B", "C", "D");
	}
}
