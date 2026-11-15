package co.surco.alertas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.surco.alertas.aplicacion.ConsultarAlertas;
import co.surco.alertas.dominio.IndicadorSequia;
import co.surco.alertas.dominio.NivelRios;
import co.surco.alertas.dominio.NivelSequia;
import co.surco.alertas.dominio.Puertos.FuenteAlertasRios;
import co.surco.alertas.dominio.Puertos.FuenteLluvia;
import co.surco.alertas.dominio.Puertos.Lluvia;
import co.surco.alertas.dominio.Puertos.SubzonaConAlerta;
import co.surco.compartido.error.ErrorDominio;
import co.surco.compartido.error.TipoError;
import java.time.LocalDate;
import java.time.Month;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class AlertasTest {

	static final LocalDate HASTA = LocalDate.of(2026, 11, 10);

	/** Lluvia igual todos los días hasta HASTA. */
	static SortedMap<LocalDate, Double> lluvia(int dias, double mmDia) {
		SortedMap<LocalDate, Double> m = new TreeMap<>();
		for (int i = 0; i < dias; i++) {
			m.put(HASTA.minusDays(i), mmDia);
		}
		return m;
	}

	/** 4 mm/día todo el año: lo normal en 30 días son 120 mm. */
	static Map<Month, Double> normal(double mmDia) {
		Map<Month, Double> m = new EnumMap<>(Month.class);
		for (Month mes : Month.values()) {
			m.put(mes, mmDia);
		}
		return m;
	}

	static FuenteAlertasRios ideam(Optional<SubzonaConAlerta> conAlerta, boolean enColombia) {
		return new FuenteAlertasRios() {
			@Override
			public Optional<SubzonaConAlerta> conAlerta(double lat, double lon) {
				return conAlerta;
			}

			@Override
			public boolean dentroDeColombia(double lat, double lon) {
				return enColombia;
			}
		};
	}

	static final FuenteLluvia SIN_LLUVIA = (lat, lon) -> {
		throw new AssertionError("no debía consultar la lluvia");
	};

	@Test
	void traduceLosNivelesDelIdeam() {
		assertThat(NivelRios.desdeIdeam("ALERTA NARANJA")).isEqualTo(NivelRios.NARANJA);
		assertThat(NivelRios.desdeIdeam(" condiciones normales ")).isEqualTo(NivelRios.NORMAL);
		assertThatThrownBy(() -> NivelRios.desdeIdeam("ALERTA MORADA")).isInstanceOfSatisfying(ErrorDominio.class,
			e -> assertThat(e.tipo()).isEqualTo(TipoError.FUENTE_INVALIDA));
	}

	@Test
	void riosConAlertaDevuelveNivelYSubzona() {
		var caso = new ConsultarAlertas(ideam(Optional.of(new SubzonaConAlerta("ALERTA ROJA", "Río Neiva")), true),
			SIN_LLUVIA);
		var a = caso.rios(2.93, -75.28);
		assertThat(a.nivel()).isEqualTo(NivelRios.ROJA);
		assertThat(a.subzona()).isEqualTo("Río Neiva");
		assertThat(a.consultadoEn()).isNotNull();
	}

	@Test
	void sinAlertaSoloSiElPuntoEstaEnColombia() {
		var enColombia = new ConsultarAlertas(ideam(Optional.empty(), true), SIN_LLUVIA).rios(2.98, -75.22);
		assertThat(enColombia.nivel()).isEqualTo(NivelRios.SIN_ALERTA);
		assertThat(enColombia.subzona()).isNull();

		// Dentro del cuadro de Colombia pero en Venezuela: el IDEAM no tiene área hidrográfica ahí
		assertThatThrownBy(() -> new ConsultarAlertas(ideam(Optional.empty(), false), SIN_LLUVIA).rios(7.0, -67.0))
			.isInstanceOfSatisfying(ErrorDominio.class, e -> assertThat(e.codigo()).isEqualTo("UBICACION_FUERA_DE_COLOMBIA"));
	}

	@Test
	void fueraDelCuadroDeColombiaNoLlamaANadie() {
		FuenteAlertasRios nunca = new FuenteAlertasRios() {
			@Override
			public Optional<SubzonaConAlerta> conAlerta(double lat, double lon) {
				throw new AssertionError("no debía llamar al IDEAM");
			}

			@Override
			public boolean dentroDeColombia(double lat, double lon) {
				throw new AssertionError("no debía llamar al IDEAM");
			}
		};
		var caso = new ConsultarAlertas(nunca, SIN_LLUVIA);
		assertThatThrownBy(() -> caso.rios(40.41, -3.70)).isInstanceOfSatisfying(ErrorDominio.class,
			e -> assertThat(e.tipo()).isEqualTo(TipoError.SOLICITUD_INVALIDA));
		assertThatThrownBy(() -> caso.sequia(Double.NaN, -75.0)).isInstanceOf(ErrorDominio.class);
	}

	@Test
	void sequiaSegunLaLluviaFrenteALoNormal() {
		assertThat(IndicadorSequia.calcular(lluvia(45, 4.0), normal(4.0)).nivel()).isEqualTo(NivelSequia.NORMAL);
		assertThat(IndicadorSequia.calcular(lluvia(45, 2.8), normal(4.0)).nivel()).isEqualTo(NivelSequia.AMARILLA);
		assertThat(IndicadorSequia.calcular(lluvia(45, 2.0), normal(4.0)).nivel()).isEqualTo(NivelSequia.NARANJA);
		assertThat(IndicadorSequia.calcular(lluvia(45, 0.4), normal(4.0)).nivel()).isEqualTo(NivelSequia.ROJA);
		assertThat(NivelSequia.desdePorcentaje(80)).isEqualTo(NivelSequia.NORMAL);
		assertThat(NivelSequia.desdePorcentaje(79.9)).isEqualTo(NivelSequia.AMARILLA);
	}

	@Test
	void losDiasSinDatoNoSesganElPorcentajeYCadaMesUsaSuNormal() {
		var conHuecos = lluvia(45, 4.0);
		conHuecos.remove(HASTA.minusDays(3));
		conHuecos.remove(HASTA.minusDays(7));
		assertThat(IndicadorSequia.calcular(conHuecos, normal(4.0)).porcentaje()).isEqualTo(100.0);

		var octubreLluvioso = normal(4.0);
		octubreLluvioso.put(Month.OCTOBER, 8.0); // ventana 12-oct a 10-nov: 20 x 8 + 10 x 4 = 200 mm
		var i = IndicadorSequia.calcular(lluvia(45, 4.0), octubreLluvioso);
		assertThat(i.normalMm()).isEqualTo(200.0);
		assertThat(i.porcentaje()).isEqualTo(60.0);
	}

	@Test
	void laEpocaSecaHabitualNoEsSequia() {
		var i = IndicadorSequia.calcular(lluvia(45, 0.0), normal(0.3));
		assertThat(i.epocaSeca()).isTrue();
		assertThat(i.nivel()).isEqualTo(NivelSequia.NORMAL);
	}

	@Test
	void sinDatosSuficientesLaFuenteNoEstaDisponible() {
		assertThatThrownBy(() -> IndicadorSequia.calcular(lluvia(10, 4.0), normal(4.0)))
			.isInstanceOfSatisfying(ErrorDominio.class, e -> assertThat(e.codigo()).isEqualTo("FUENTE_NO_DISPONIBLE"));
		assertThatThrownBy(() -> IndicadorSequia.calcular(new TreeMap<>(), normal(4.0))).isInstanceOf(ErrorDominio.class);
	}

	@Test
	void consultaLaSequiaConLaLluviaDeLaFuente() {
		var caso = new ConsultarAlertas(ideam(Optional.empty(), true), (lat, lon) -> new Lluvia(lluvia(45, 2.0), normal(4.0)));
		var s = caso.sequia(2.93, -75.28);
		assertThat(s.nivel()).isEqualTo(NivelSequia.NARANJA);
		assertThat(s.lat()).isEqualTo(2.93);
	}
}
