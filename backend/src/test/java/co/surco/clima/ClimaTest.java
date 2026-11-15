package co.surco.clima;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.surco.clima.aplicacion.ConsultarClima;
import co.surco.clima.dominio.Avisos;
import co.surco.clima.dominio.Cielo;
import co.surco.clima.dominio.ErroresClima.ClimaNoDisponible;
import co.surco.clima.dominio.Nivel;
import co.surco.clima.dominio.Puertos.AlertaRio;
import co.surco.clima.dominio.Puertos.DatosSatelite;
import co.surco.clima.dominio.Puertos.FuenteAlertasRios;
import co.surco.clima.dominio.Puertos.FuenteSatelital;
import co.surco.clima.dominio.Puertos.FuenteTiempo;
import co.surco.clima.dominio.Puertos.Tiempo;
import co.surco.clima.dominio.Puertos.ValorDiario;
import co.surco.clima.dominio.ResumenClima;
import co.surco.clima.dominio.ResumenClima.Actual;
import co.surco.clima.dominio.ResumenClima.Aviso;
import co.surco.clima.dominio.ResumenClima.Dia;
import co.surco.compartido.error.ErrorDominio;
import co.surco.territorio.ConsultaTerritorio;
import co.surco.territorio.Departamento;
import co.surco.territorio.Municipio;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Dominio y caso de uso del módulo clima, con fuentes de mentira: no sale a la red. */
class ClimaTest {

	/** Domingo. */
	private static final LocalDate HOY = LocalDate.of(2026, 11, 15);
	private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-15T15:00:00Z"), ZoneOffset.UTC);
	private static final Municipio NEIVA = new Municipio("41001", "Neiva", "Municipio", 2.9273, -75.2819);
	private static final Map<Month, Double> NORMAL = Map.of(Month.OCTOBER, 5.0, Month.NOVEMBER, 5.0);

	private static final ConsultaTerritorio TERRITORIO = new ConsultaTerritorio() {
		@Override
		public List<Departamento> departamentos() {
			return List.of(new Departamento("41", "Huila"));
		}

		@Override
		public List<Municipio> municipiosDe(String codigoDepartamento) {
			return List.of(NEIVA);
		}

		@Override
		public Optional<Municipio> municipio(String codigo) {
			return NEIVA.codigo().equals(codigo) ? Optional.of(NEIVA) : Optional.empty();
		}
	};

	private static Tiempo tiempo(double... lluviaPorDia) {
		Actual actual = new Actual(29, 32.0, "Parcialmente nublado", "cloudsun", 68, 11.0, "NE", 24.0, 12.4, 86);
		List<Dia> dias = new ArrayList<>();
		for (int i = 0; i < lluviaPorDia.length; i++) {
			dias.add(new Dia(HOY.plusDays(i), "Lluvia", "rain", 30, 22, lluviaPorDia[i], 80, 8.0,
				lluviaPorDia[i] >= Avisos.LLUVIA_FUERTE_MM));
		}
		return new Tiempo(actual, dias, 442.0);
	}

	/** 30 días seguidos hasta el 12 de noviembre con la misma lluvia. Lo normal (NORMAL) son 5 mm por día. */
	private static SortedMap<LocalDate, Double> lluvia(double mmPorDia) {
		SortedMap<LocalDate, Double> lluvia = new TreeMap<>();
		for (int i = 0; i < 30; i++) {
			lluvia.put(HOY.minusDays(3 + i), mmPorDia);
		}
		return lluvia;
	}

	private static DatosSatelite satelite() {
		return new DatosSatelite(new ValorDiario(HOY.minusDays(5), 18.0), new ValorDiario(HOY.minusDays(3), 0.71),
			lluvia(5), NORMAL);
	}

	private static ConsultarClima servicio(FuenteTiempo tiempo, FuenteSatelital satelite, FuenteAlertasRios rios) {
		return new ConsultarClima(TERRITORIO, tiempo, satelite, rios, RELOJ);
	}

	@Test
	void armaElResumenConLasTresFuentes() {
		ResumenClima resumen = servicio((lat, lon) -> tiempo(12, 38, 24, 6, 0, 0, 9), (lat, lon) -> satelite(),
			(lat, lon) -> Optional.of(new AlertaRio("ALERTA NARANJA", "Río Neiva"))).resumen("41001", null, null);

		assertThat(resumen.ubicacion().municipio()).isEqualTo("Neiva");
		assertThat(resumen.ubicacion().departamento()).isEqualTo("Huila");
		assertThat(resumen.ubicacion().lat()).isEqualTo(2.9273);
		assertThat(resumen.actualizado()).hasToString("2026-11-15T10:00-05:00");
		assertThat(resumen.pronostico()).hasSize(7);
		assertThat(resumen.satelite().radiacionKwh()).isEqualTo(5.0); // 18 MJ/m² entre 3,6
		assertThat(resumen.satelite().humedadSueloPct()).isEqualTo(71.0);
		assertThat(resumen.fuentes()).containsExactly("Open-Meteo", "NASA POWER", "IDEAM");
		// Primero la más grave: la naranja del IDEAM, luego la lluvia fuerte de mañana (38 mm)
		assertThat(resumen.avisos()).extracting(Aviso::tipo).containsExactly("RIOS", "LLUVIA_FUERTE");
		assertThat(resumen.avisos().get(0).oficial()).isTrue();
		assertThat(resumen.avisos().get(0).zona()).isEqualTo("Río Neiva");
		assertThat(resumen.avisos().get(1).titulo()).isEqualTo("Lluvia fuerte el lunes");
	}

	@Test
	void siNasaOElIdeamFallanElResumenSaleSinEsaParte() {
		ResumenClima resumen = servicio((lat, lon) -> tiempo(0, 2, 0), (lat, lon) -> {
			throw new IllegalStateException("sin red");
		}, (lat, lon) -> {
			throw new IllegalStateException("sin red");
		}).resumen("41001", null, null);

		assertThat(resumen.satelite()).isNull();
		assertThat(resumen.avisos()).isEmpty();
		assertThat(resumen.fuentes()).containsExactly("Open-Meteo");
	}

	@Test
	void sinPronosticoRespondeNoDisponible() {
		ConsultarClima servicio = servicio((lat, lon) -> {
			throw new IllegalStateException("sin red");
		}, (lat, lon) -> satelite(), (lat, lon) -> Optional.empty());

		assertThatThrownBy(() -> servicio.resumen("41001", null, null)).isInstanceOf(ClimaNoDisponible.class);
	}

	@Test
	void usaLasCoordenadasDelTerrenoSiLlegan() {
		List<Double> consultadas = new ArrayList<>();
		ResumenClima resumen = servicio((lat, lon) -> {
			consultadas.add(lat);
			return tiempo(0);
		}, (lat, lon) -> satelite(), (lat, lon) -> Optional.empty()).resumen("41001", 2.5, -75.5);

		assertThat(consultadas).containsExactly(2.5);
		assertThat(resumen.ubicacion().lon()).isEqualTo(-75.5);
		assertThat(resumen.ubicacion().municipio()).isEqualTo("Neiva");
	}

	@Test
	void guardaLaRespuestaParaNoConsultarEnCadaRecarga() {
		AtomicInteger consultas = new AtomicInteger();
		ConsultarClima servicio = servicio((lat, lon) -> {
			consultas.incrementAndGet();
			return tiempo(0);
		}, (lat, lon) -> satelite(), (lat, lon) -> Optional.empty());

		servicio.resumen("41001", null, null);
		servicio.resumen("41001", null, null);

		assertThat(consultas).hasValue(1);
	}

	@Test
	void validaLaUbicacion() {
		ConsultarClima servicio = servicio((lat, lon) -> tiempo(0), (lat, lon) -> satelite(),
			(lat, lon) -> Optional.empty());

		assertThatThrownBy(() -> servicio.resumen(null, null, null)).isInstanceOf(ErrorDominio.class)
			.extracting(e -> ((ErrorDominio) e).codigo()).isEqualTo("CLIMA_UBICACION_REQUERIDA");
		assertThatThrownBy(() -> servicio.resumen("99999", null, null)).isInstanceOf(ErrorDominio.class)
			.extracting(e -> ((ErrorDominio) e).codigo()).isEqualTo("MUNICIPIO_NO_ENCONTRADO");
		assertThatThrownBy(() -> servicio.resumen(null, 40.4, -3.7)).isInstanceOf(ErrorDominio.class)
			.extracting(e -> ((ErrorDominio) e).codigo()).isEqualTo("CLIMA_FUERA_DE_COLOMBIA");
	}

	@Test
	void avisaDeSequiaSegunCuantoLlovioFrenteALoNormal() {
		assertThat(Avisos.deSequia(lluvia(5), NORMAL, HOY, "NASA POWER")).isEmpty();
		assertThat(Avisos.deSequia(lluvia(3.5), NORMAL, HOY, "NASA POWER"))
			.hasValueSatisfying(a -> assertThat(a.nivel()).isEqualTo(Nivel.AMARILLA));
		assertThat(Avisos.deSequia(lluvia(2.5), NORMAL, HOY, "NASA POWER"))
			.hasValueSatisfying(a -> assertThat(a.nivel()).isEqualTo(Nivel.NARANJA));
		assertThat(Avisos.deSequia(lluvia(0.5), NORMAL, HOY, "NASA POWER")).hasValueSatisfying(a -> {
			assertThat(a.nivel()).isEqualTo(Nivel.ROJA);
			assertThat(a.oficial()).isFalse();
		});
		// Época seca habitual (lo normal es casi nada): no es sequía
		assertThat(Avisos.deSequia(lluvia(0), Map.of(Month.OCTOBER, 0.3, Month.NOVEMBER, 0.3), HOY, "NASA POWER"))
			.isEmpty();
		// Datos viejos: no sirven para avisar hoy
		assertThat(Avisos.deSequia(lluvia(0.5), NORMAL, HOY.plusDays(30), "NASA POWER")).isEmpty();
	}

	@Test
	void noInventaUnaAlertaSiElIdeamMandaUnNivelDesconocido() {
		assertThat(Avisos.deRios(new AlertaRio("ALERTA ROJA", "Río Neiva")))
			.hasValueSatisfying(a -> assertThat(a.nivel()).isEqualTo(Nivel.ROJA));
		assertThat(Avisos.deRios(new AlertaRio("CONDICIONES NORMALES", "Río Neiva"))).isEmpty();
		assertThat(Avisos.deRios(new AlertaRio(null, "Río Neiva"))).isEmpty();
	}

	@Test
	void traduceElCodigoDelTiempo() {
		assertThat(Cielo.descripcion(2)).isEqualTo("Parcialmente nublado");
		assertThat(Cielo.icono(0)).isEqualTo("sun");
		assertThat(Cielo.icono(95)).isEqualTo("rain");
		// Una llovizna de 0,2 mm no convierte el día en lluvioso
		assertThat(Cielo.iconoDelDia(51, 0.2)).isEqualTo("cloudsun");
		assertThat(Cielo.iconoDelDia(3, 12)).isEqualTo("rain");
		assertThat(Cielo.rumbo(261)).isEqualTo("O");
		assertThat(Cielo.rumbo(350)).isEqualTo("N");
	}
}
