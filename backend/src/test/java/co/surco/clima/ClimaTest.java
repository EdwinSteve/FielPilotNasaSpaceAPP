package co.surco.clima;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.surco.clima.aplicacion.ConsultarClimaActual;
import co.surco.clima.dominio.Condicion;
import co.surco.clima.dominio.Puertos.ClimaAhora;
import co.surco.clima.dominio.Puertos.DatoDiario;
import co.surco.clima.dominio.Puertos.FuenteClimaAhora;
import co.surco.clima.dominio.Puertos.FuenteSueloYSol;
import co.surco.clima.dominio.Puertos.SueloYSol;
import co.surco.clima.dominio.Tarjetas;
import co.surco.compartido.error.ErrorDominio;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ClimaTest {

	static final ClimaAhora AHORA = new ClimaAhora(29.04, 1.5, 12.4, 0.6, 40, 68, 22.3, 11.0, 45, 24.0, 9,
		"2026-11-14T16:04:12-05:00");
	static final SueloYSol SUELO_Y_SOL = new SueloYSol(new DatoDiario(LocalDate.of(2026, 11, 10), 20.16),
		new DatoDiario(LocalDate.of(2026, 11, 12), 0.62));

	static Condicion tarjeta(List<Condicion> tarjetas, String variable) {
		return tarjetas.stream().filter(c -> c.variable().equals(variable)).findFirst().orElseThrow();
	}

	@Test
	void armaLasSeisTarjetasEnElOrdenDelTablero() {
		List<Condicion> t = Tarjetas.armar(AHORA, SUELO_Y_SOL);
		assertThat(t.stream().map(Condicion::variable).toList()).isEqualTo(List.of("TEMPERATURA_AIRE",
			"PRECIPITACION_24H", "HUMEDAD_RELATIVA", "VIENTO", "RADIACION_SOLAR", "HUMEDAD_SUELO"));

		Condicion temperatura = tarjeta(t, "TEMPERATURA_AIRE");
		assertThat(temperatura.valor()).isEqualTo(29.0);
		assertThat(temperatura.detalle()).isEqualTo("+1,5 °C vs. ayer a esta hora");
		assertThat(temperatura.fuente()).isEqualTo("Google Weather");
		assertThat(tarjeta(t, "PRECIPITACION_24H").detalle()).isEqualTo("Última hora: 0,6 mm · probabilidad de lluvia 40 %");
		assertThat(tarjeta(t, "HUMEDAD_RELATIVA").detalle()).isEqualTo("Punto de rocío: 22,3 °C");
		assertThat(tarjeta(t, "VIENTO").detalle()).isEqualTo("Dirección NE · ráfagas 24 km/h");

		Condicion radiacion = tarjeta(t, "RADIACION_SOLAR");
		assertThat(radiacion.valor()).isEqualTo(5.6); // 20,16 MJ/m² ÷ 3,6
		assertThat(radiacion.detalle()).isEqualTo("Total del día · índice UV 9");
		assertThat(radiacion.medido()).isEqualTo("2026-11-10");
		assertThat(tarjeta(t, "HUMEDAD_SUELO").valor()).isEqualTo(62.0);
	}

	@Test
	void unaFuenteCaidaSoloVaciaSusTarjetas() {
		List<Condicion> sinGoogle = Tarjetas.armar(null, SUELO_Y_SOL);
		assertThat(tarjeta(sinGoogle, "TEMPERATURA_AIRE").valor()).isNull();
		assertThat(tarjeta(sinGoogle, "TEMPERATURA_AIRE").detalle()).isEqualTo("Sin dato reciente");
		assertThat(tarjeta(sinGoogle, "RADIACION_SOLAR").detalle()).isEqualTo("Total del día");
		assertThat(tarjeta(sinGoogle, "HUMEDAD_SUELO").valor()).isEqualTo(62.0);

		List<Condicion> sinNasa = Tarjetas.armar(AHORA, null);
		assertThat(tarjeta(sinNasa, "VIENTO").valor()).isEqualTo(11.0);
		assertThat(tarjeta(sinNasa, "RADIACION_SOLAR").valor()).isNull();
	}

	@Test
	void rumbosDelViento() {
		assertThat(Tarjetas.rumbo(0)).isEqualTo("N");
		assertThat(Tarjetas.rumbo(100)).isEqualTo("E");
		assertThat(Tarjetas.rumbo(225)).isEqualTo("SO");
		assertThat(Tarjetas.rumbo(350)).isEqualTo("N");
		assertThat(Tarjetas.rumbo(-45)).isEqualTo("NO");
	}

	@Test
	void guardaLaRespuestaDiezMinutosPorPunto() {
		AtomicInteger llamadas = new AtomicInteger();
		FuenteClimaAhora google = (lat, lon) -> {
			llamadas.incrementAndGet();
			return AHORA;
		};
		var caso = new ConsultarClimaActual(google, (lat, lon) -> SUELO_Y_SOL);
		caso.en(2.93, -75.28);
		caso.en(2.931, -75.281);
		assertThat(llamadas.get()).isEqualTo(1);
		assertThat(caso.en(2.93, -75.28).condiciones().size()).isEqualTo(6);
	}

	@Test
	void conTodasLasFuentesCaidasRespondeSeisTarjetasVacias() {
		FuenteClimaAhora rota = (lat, lon) -> {
			throw new IllegalStateException("Falta la clave");
		};
		FuenteSueloYSol rota2 = (lat, lon) -> {
			throw new IllegalStateException("sin red");
		};
		var r = new ConsultarClimaActual(rota, rota2).en(2.93, -75.28);
		assertThat(r.condiciones().size()).isEqualTo(6);
		assertThat(r.condiciones().stream().noneMatch(Condicion::tieneDato)).isTrue();
	}

	@Test
	void fueraDeColombiaEs400() {
		var caso = new ConsultarClimaActual((lat, lon) -> AHORA, (lat, lon) -> SUELO_Y_SOL);
		assertThatThrownBy(() -> caso.en(40.41, -3.70)).isInstanceOfSatisfying(ErrorDominio.class,
			e -> assertThat(e.codigo()).isEqualTo("UBICACION_FUERA_DE_COLOMBIA"));
	}
}
