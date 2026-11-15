package co.surco.clima.infraestructura.google;

import co.surco.clima.dominio.ErroresClima;
import co.surco.clima.dominio.Puertos.ClimaAhora;
import co.surco.clima.dominio.Puertos.FuenteClimaAhora;
import co.surco.clima.dominio.Tarjetas;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Weather API de Google Maps Platform, {@code currentConditions:lookup}. La clave se lee de
 * {@code surco.google-weather.api-key} (variable GOOGLE_WEATHER_API_KEY) y nunca va en el código.
 *
 * <p>La API de SURCO usa snake_case en el JSON, pero Google responde en camelCase: por eso cada campo de
 * varias palabras lleva su {@code @JsonProperty}.
 */
@Component
class ClienteGoogleWeather implements FuenteClimaAhora {

	private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

	private final RestClient http;
	private final String clave;

	ClienteGoogleWeather(RestClient.Builder builder, @Value("${surco.google-weather.api-key:}") String clave,
		@Value("${surco.google-weather.url:https://weather.googleapis.com/v1}") String url) {
		var fabrica = new SimpleClientHttpRequestFactory();
		fabrica.setConnectTimeout(Duration.ofSeconds(5));
		fabrica.setReadTimeout(Duration.ofSeconds(10));
		this.http = builder.baseUrl(url).requestFactory(fabrica).build();
		this.clave = clave;
	}

	@Override
	public ClimaAhora actual(double lat, double lon) {
		if (clave == null || clave.isBlank()) {
			throw new IllegalStateException("Falta la clave de Google Weather (GOOGLE_WEATHER_API_KEY)");
		}
		Condiciones c;
		try {
			c = http.get().uri(uri -> uri.path("/currentConditions:lookup")
				.queryParam("key", "{clave}")
				.queryParam("location.latitude", String.format(Locale.ROOT, "%.5f", lat))
				.queryParam("location.longitude", String.format(Locale.ROOT, "%.5f", lon))
				.queryParam("unitsSystem", "METRIC")
				.build(clave)).retrieve().body(Condiciones.class);
		} catch (RestClientException e) {
			throw ErroresClima.fuenteNoDisponible(Tarjetas.GOOGLE);
		}
		if (c == null || c.temperature() == null) {
			throw ErroresClima.fuenteNoDisponible(Tarjetas.GOOGLE);
		}
		Historial h = c.historial();
		Precipitacion p = c.precipitation();
		Viento v = c.wind();
		return new ClimaAhora(
			c.temperature().degrees(),
			h == null || h.temperatureChange() == null ? null : h.temperatureChange().degrees(),
			h == null || h.qpf() == null ? null : h.qpf().quantity(),
			p == null || p.qpf() == null ? null : p.qpf().quantity(),
			p == null || p.probability() == null ? null : p.probability().percent(),
			c.relativeHumidity(),
			c.dewPoint() == null ? null : c.dewPoint().degrees(),
			v == null || v.speed() == null ? null : v.speed().value(),
			v == null || v.direction() == null ? null : v.direction().degrees(),
			v == null || v.gust() == null ? null : v.gust().value(),
			c.uvIndex(),
			enColombia(c.currentTime()));
	}

	/** La hora de Google viene en UTC; se entrega en hora de Colombia. */
	static String enColombia(String utc) {
		try {
			return Instant.parse(utc).atZone(COLOMBIA).toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS).toString();
		} catch (RuntimeException e) {
			return utc;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Condiciones(
		@JsonProperty("currentTime") String currentTime,
		@JsonProperty("temperature") Temperatura temperature,
		@JsonProperty("dewPoint") Temperatura dewPoint,
		@JsonProperty("relativeHumidity") Integer relativeHumidity,
		@JsonProperty("uvIndex") Integer uvIndex,
		@JsonProperty("precipitation") Precipitacion precipitation,
		@JsonProperty("wind") Viento wind,
		@JsonProperty("currentConditionsHistory") Historial historial) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Temperatura(@JsonProperty("degrees") Double degrees) {
	}

	/** qpf: lo acumulado en la última hora. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record Precipitacion(@JsonProperty("probability") Probabilidad probability, @JsonProperty("qpf") Cantidad qpf) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Probabilidad(@JsonProperty("percent") Integer percent) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Cantidad(@JsonProperty("quantity") Double quantity) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Viento(@JsonProperty("direction") Direccion direction, @JsonProperty("speed") Velocidad speed,
		@JsonProperty("gust") Velocidad gust) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Direccion(@JsonProperty("degrees") Integer degrees) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Velocidad(@JsonProperty("value") Double value) {
	}

	/** Cambios en las últimas 24 horas; qpf es la lluvia acumulada en ese periodo. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record Historial(@JsonProperty("temperatureChange") Temperatura temperatureChange,
		@JsonProperty("qpf") Cantidad qpf) {
	}
}
