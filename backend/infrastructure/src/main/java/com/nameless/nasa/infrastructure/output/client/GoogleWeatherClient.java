package com.nameless.nasa.infrastructure.output.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Locale;

/**
 * Condiciones actuales de la Weather API de Google Maps Platform.
 * Necesita una clave de API: propiedad google.weather.api-key
 * (o la variable de entorno GOOGLE_WEATHER_API_KEY). La clave nunca va en el código.
 */
@Component
public class GoogleWeatherClient {
    private static final String BASE = "https://weather.googleapis.com/v1";

    private final RestClient http;
    private final String apiKey;

    public GoogleWeatherClient(@Value("${google.weather.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(10_000);
        this.http = RestClient.builder().baseUrl(BASE).requestFactory(fabrica).build();
    }

    /** @throws IllegalStateException si falta la clave o Google responde sin datos */
    public Condiciones condicionesActuales(double lat, double lon) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Falta la clave de Google Weather (google.weather.api-key o GOOGLE_WEATHER_API_KEY)");
        }
        Condiciones condiciones = http.get()
                .uri(uri -> uri.path("/currentConditions:lookup")
                        .queryParam("key", "{clave}")
                        .queryParam("location.latitude", String.format(Locale.ROOT, "%.5f", lat))
                        .queryParam("location.longitude", String.format(Locale.ROOT, "%.5f", lon))
                        .queryParam("unitsSystem", "METRIC")
                        .build(apiKey))
                .retrieve()
                .body(Condiciones.class);

        if (condiciones == null || condiciones.temperature() == null) {
            throw new IllegalStateException("Google Weather respondió sin condiciones");
        }
        return condiciones;
    }

    /** Solo los campos que usa el tablero; el resto de la respuesta de Google se ignora. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Condiciones(
            String currentTime,
            Temperatura temperature,
            Temperatura dewPoint,
            Integer relativeHumidity,
            Integer uvIndex,
            Precipitacion precipitation,
            Viento wind,
            Historial currentConditionsHistory) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Temperatura(Double degrees, String unit) {}

    /** qpf es lo acumulado en la última hora. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Precipitacion(Probabilidad probability, Cantidad qpf) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Probabilidad(Integer percent, String type) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cantidad(Double quantity, String unit) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Viento(Direccion direction, Velocidad speed, Velocidad gust) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Direccion(Integer degrees, String cardinal) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Velocidad(Double value, String unit) {}

    /** Cambios en las últimas 24 horas; qpf es la lluvia acumulada en ese periodo. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Historial(Temperatura temperatureChange, Cantidad qpf) {}
}
