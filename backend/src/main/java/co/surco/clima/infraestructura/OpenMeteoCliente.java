package co.surco.clima.infraestructura;

import static co.surco.clima.infraestructura.ClienteHttp.numero;

import co.surco.clima.dominio.Avisos;
import co.surco.clima.dominio.Cielo;
import co.surco.clima.dominio.Puertos.FuenteTiempo;
import co.surco.clima.dominio.Puertos.Tiempo;
import co.surco.clima.dominio.ResumenClima.Actual;
import co.surco.clima.dominio.ResumenClima.Dia;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Condiciones actuales y pronóstico de 7 días de Open-Meteo (https://open-meteo.com): no necesita clave.
 * Una sola consulta trae también la lluvia por hora de la última semana.
 */
@Component
class OpenMeteoCliente implements FuenteTiempo {

	private static final String URL = "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
		+ "&timezone=America%%2FBogota&past_days=7&forecast_days=7"
		+ "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,"
		+ "wind_direction_10m,wind_gusts_10m"
		+ "&hourly=precipitation"
		+ "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,"
		+ "precipitation_probability_max,uv_index_max";

	private final ClienteHttp http;

	OpenMeteoCliente(ObjectMapper json) {
		this.http = new ClienteHttp(json, Duration.ofSeconds(10));
	}

	@Override
	public Tiempo consultar(double lat, double lon) {
		JsonNode raiz = http.get(String.format(Locale.ROOT, URL, lat, lon));
		JsonNode actual = raiz.path("current");
		Double temperatura = numero(actual.path("temperature_2m"));
		if (temperatura == null) {
			throw new IllegalStateException("Open-Meteo respondió sin condiciones actuales");
		}
		LocalDateTime ahora = LocalDateTime.parse(actual.path("time").asString());

		// Lluvia por hora (hora local): se suma lo que ya cayó en las últimas 24 horas y en los últimos 7 días
		JsonNode horas = raiz.path("hourly").path("time");
		JsonNode lluviaHora = raiz.path("hourly").path("precipitation");
		double lluviaDia = 0;
		double lluviaSemana = 0;
		for (int i = 0; i < horas.size(); i++) {
			LocalDateTime hora = LocalDateTime.parse(horas.get(i).asString());
			Double mm = numero(lluviaHora.get(i));
			if (mm == null || hora.isAfter(ahora)) {
				continue;
			}
			if (hora.isAfter(ahora.minusDays(7))) {
				lluviaSemana += mm;
			}
			if (hora.isAfter(ahora.minusHours(24))) {
				lluviaDia += mm;
			}
		}

		int codigo = actual.path("weather_code").asInt();
		Double humedad = numero(actual.path("relative_humidity_2m"));
		Double direccion = numero(actual.path("wind_direction_10m"));
		Actual condiciones = new Actual(temperatura, numero(actual.path("apparent_temperature")),
			Cielo.descripcion(codigo), Cielo.icono(codigo), humedad == null ? null : (int) Math.round(humedad),
			numero(actual.path("wind_speed_10m")), direccion == null ? null : Cielo.rumbo(direccion),
			numero(actual.path("wind_gusts_10m")), redondear(lluviaDia), redondear(lluviaSemana));

		return new Tiempo(condiciones, dias(raiz.path("daily"), ahora.toLocalDate()), numero(raiz.path("elevation")));
	}

	/** La respuesta trae también los 7 días anteriores: aquí solo quedan los de hoy en adelante. */
	private static List<Dia> dias(JsonNode diario, LocalDate hoy) {
		List<Dia> dias = new ArrayList<>();
		JsonNode fechas = diario.path("time");
		for (int i = 0; i < fechas.size(); i++) {
			LocalDate fecha = LocalDate.parse(fechas.get(i).asString());
			Double maxima = numero(diario.path("temperature_2m_max").get(i));
			Double minima = numero(diario.path("temperature_2m_min").get(i));
			if (fecha.isBefore(hoy) || maxima == null || minima == null) {
				continue;
			}
			int codigo = diario.path("weather_code").path(i).asInt();
			Double lluvia = numero(diario.path("precipitation_sum").get(i));
			double lluviaMm = lluvia == null ? 0 : lluvia;
			Double probabilidad = numero(diario.path("precipitation_probability_max").get(i));
			dias.add(new Dia(fecha, Cielo.descripcion(codigo), Cielo.iconoDelDia(codigo, lluviaMm), maxima, minima,
				lluviaMm, probabilidad == null ? null : (int) Math.round(probabilidad),
				numero(diario.path("uv_index_max").get(i)), lluviaMm >= Avisos.LLUVIA_FUERTE_MM));
		}
		if (dias.isEmpty()) {
			throw new IllegalStateException("Open-Meteo respondió sin pronóstico diario");
		}
		return dias;
	}

	private static double redondear(double valor) {
		return Math.round(valor * 10) / 10.0;
	}
}
