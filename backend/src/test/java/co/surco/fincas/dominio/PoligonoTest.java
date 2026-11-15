package co.surco.fincas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import co.surco.compartido.error.ErrorDominio;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PoligonoTest {

	/** Un cuadrado de lado `grados` con la esquina suroccidental en (lat, lon). */
	static List<Coordenada> cuadrado(double lat, double lon, double grados) {
		return List.of(new Coordenada(lat, lon), new Coordenada(lat, lon + grados),
			new Coordenada(lat + grados, lon + grados), new Coordenada(lat + grados, lon));
	}

	static String codigo(Runnable r) {
		try {
			r.run();
		} catch (ErrorDominio e) {
			return e.codigo();
		}
		return "sin error";
	}

	@Test
	void areaYCentroideDeUnCuadradoCercaDelEcuador() {
		// 0,001° ≈ 111,2 m de lado en latitud 4,6: unas 1,236 ha
		Poligono p = Poligono.de(cuadrado(4.6, -74.08, 0.001));
		double lado = Math.toRadians(0.001) * 6_371_008.8;
		double esperada = lado * lado * Math.cos(Math.toRadians(4.6005)) / 10_000;
		assertThat(p.areaHa()).isCloseTo(esperada, within(esperada * 0.001));
		assertThat(p.centroide().lat()).isCloseTo(4.6005, within(1e-7));
		assertThat(p.centroide().lon()).isCloseTo(-74.0795, within(1e-7));
	}

	@Test
	void elSentidoDelDibujoNoCambiaElArea() {
		List<Coordenada> horario = new ArrayList<>(cuadrado(5.0, -75.5, 0.002));
		double area = Poligono.de(horario).areaHa();
		java.util.Collections.reverse(horario);
		assertThat(Poligono.de(horario).areaHa()).isCloseTo(area, within(1e-9));
	}

	@Test
	void aceptaElAnilloCerradoYQuitaDoblesClics() {
		List<Coordenada> puntos = new ArrayList<>(cuadrado(5.0, -75.5, 0.002));
		puntos.add(1, puntos.get(0)); // doble clic
		puntos.add(puntos.get(0)); // anillo cerrado
		assertThat(Poligono.de(puntos).vertices()).hasSize(4);
	}

	@Test
	void geoJsonIdaYVuelta() {
		Poligono p = Poligono.de(cuadrado(5.0, -75.5, 0.002));
		String g = p.aGeoJson();
		assertThat(g).startsWith("{\"type\":\"Polygon\",\"coordinates\":[[[-75.5000000,5.0000000],");
		assertThat(g).endsWith("[-75.5000000,5.0000000]]]}"); // anillo cerrado, [lon, lat]
		Poligono otra = Poligono.desdeGeoJson(g);
		assertThat(otra.vertices()).isEqualTo(p.vertices());
		assertThat(otra.areaHa()).isCloseTo(p.areaHa(), within(1e-9));
	}

	@Test
	void rechazaPocosPuntos() {
		assertThat(codigo(() -> Poligono.de(List.of(new Coordenada(5, -75), new Coordenada(5.001, -75)))))
			.isEqualTo("FINCAS_POLIGONO_INCOMPLETO");
		assertThat(codigo(() -> Poligono.de(null))).isEqualTo("FINCAS_POLIGONO_INCOMPLETO");
	}

	@Test
	void rechazaUnMonoBowtieConLadosCruzados() {
		List<Coordenada> corbatin = List.of(new Coordenada(5.0, -75.5), new Coordenada(5.002, -75.498),
			new Coordenada(5.0, -75.498), new Coordenada(5.002, -75.5));
		assertThat(codigo(() -> Poligono.de(corbatin))).isEqualTo("FINCAS_POLIGONO_CRUZADO");
	}

	@Test
	void rechazaPuntosFueraDeColombia() {
		assertThat(codigo(() -> Poligono.de(cuadrado(40.4, -3.7, 0.002)))).isEqualTo("FINCAS_FUERA_DE_COLOMBIA");
	}

	@Test
	void rechazaAreasAbsurdas() {
		assertThat(codigo(() -> Poligono.de(cuadrado(5.0, -75.5, 0.00005)))).isEqualTo("FINCAS_AREA_MUY_PEQUENA");
		assertThat(codigo(() -> Poligono.de(cuadrado(2.0, -74.0, 2.0)))).isEqualTo("FINCAS_AREA_MUY_GRANDE");
	}

	@Test
	void rechazaDemasiadosPuntos() {
		List<Coordenada> circulo = new ArrayList<>();
		for (int i = 0; i < 201; i++) {
			double a = 2 * Math.PI * i / 201;
			circulo.add(new Coordenada(5.0 + 0.01 * Math.sin(a), -75.5 + 0.01 * Math.cos(a)));
		}
		assertThat(codigo(() -> Poligono.de(circulo))).isEqualTo("FINCAS_POLIGONO_DEMASIADOS_PUNTOS");
	}

	@Test
	void coordenadaInvalida() {
		assertThatThrownBy(() -> new Coordenada(Double.NaN, -75)).isInstanceOf(ErrorDominio.class);
		assertThatThrownBy(() -> new Coordenada(95, -75)).isInstanceOf(ErrorDominio.class);
	}

	@Test
	void nombreDelTerreno() {
		Poligono p = Poligono.de(cuadrado(5.0, -75.5, 0.002));
		Terreno t = Terreno.nuevo(java.util.UUID.randomUUID(), "  Lote   de  arriba ", "17001", true, p,
			java.time.Instant.EPOCH);
		assertThat(t.nombre()).isEqualTo("Lote de arriba");
		assertThat(t.departamento()).isEqualTo("17");
		assertThat(codigo(() -> Terreno.nuevo(t.usuarioId(), "  ", "17001", false, p, java.time.Instant.EPOCH)))
			.isEqualTo("FINCAS_NOMBRE_INVALIDO");
	}
}
