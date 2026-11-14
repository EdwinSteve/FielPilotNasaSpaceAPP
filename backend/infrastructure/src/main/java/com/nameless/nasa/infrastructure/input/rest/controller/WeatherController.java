package com.nameless.nasa.infrastructure.input.rest.controller;

import com.nameless.nasa.infrastructure.input.rest.response.WeatherConditionResponse;
import com.nameless.nasa.infrastructure.input.rest.response.WeatherConditionsResponse;
import com.nameless.nasa.infrastructure.output.client.GoogleWeatherClient;
import com.nameless.nasa.infrastructure.output.client.GoogleWeatherClient.Condiciones;
import com.nameless.nasa.infrastructure.output.client.NasaPowerClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GET /api/weather/current?lat=2.93&lon=-75.28
 * Condiciones actuales para el tablero: Google Weather y, para lo que Google no trae, NASA POWER.
 */
@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
    private static final Locale ES = Locale.forLanguageTag("es-CO");   // decimales con coma en los textos
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(WeatherController.class);

    private static final String GOOGLE = "Google Weather";
    private static final String NASA = "NASA POWER";
    private static final String RADIACION = "ALLSKY_SFC_SW_DWN";   // MJ/m² por día
    private static final String SUELO = "GWETROOT";                // 0 a 1, zona de raíces
    /** Evita gastar una consulta de Google por cada recarga del tablero. */
    private static final Duration VIGENCIA_CACHE = Duration.ofMinutes(10);

    private final GoogleWeatherClient google;
    private final NasaPowerClient power;
    private final Map<String, EnCache> cache = new ConcurrentHashMap<>();

    private record EnCache(Instant momento, List<WeatherConditionResponse> condiciones) {}

    WeatherController(GoogleWeatherClient google, NasaPowerClient power) {
        this.google = google;
        this.power = power;
    }

    @GetMapping("/current")
    public WeatherConditionsResponse currentWeatherConditions(
            @RequestParam double lat, @RequestParam double lon) {
        if (lat < -4.5 || lat > 13.5 || lon < -82 || lon > -66) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La ubicación está fuera de Colombia.");
        }

        String clave = String.format(Locale.ROOT, "%.2f,%.2f", lat, lon);
        EnCache guardado = cache.get(clave);
        List<WeatherConditionResponse> condiciones;
        if (guardado != null && guardado.momento().plus(VIGENCIA_CACHE).isAfter(Instant.now())) {
            condiciones = guardado.condiciones();
        } else {
            condiciones = consultarTodo(lat, lon);
            // Solo se guarda si al menos una tarjeta trajo dato.
            if (condiciones.stream().anyMatch(c -> c.valor() != null)) {
                cache.put(clave, new EnCache(Instant.now(), condiciones));
            }
        }
        return new WeatherConditionsResponse(lat, lon, OffsetDateTime.now(COLOMBIA), condiciones);
    }

    /** Google y la NASA se consultan al mismo tiempo; si una falla, solo sus tarjetas llegan vacías. */
    private List<WeatherConditionResponse> consultarTodo(double lat, double lon) {
        CompletableFuture<Condiciones> deGoogle = CompletableFuture.supplyAsync(() -> {
            try {
                return google.condicionesActuales(lat, lon);
            } catch (RuntimeException e) {
                log.warn("Fallo consultando a Google Weather: {}", e.toString());
                return null;
            }
        });
        CompletableFuture<Map<String, NasaPowerClient.ValorDiario>> deNasa = CompletableFuture.supplyAsync(() -> {
            try {
                return power.ultimosValores(lat, lon, RADIACION, SUELO);
            } catch (RuntimeException e) {
                log.warn("Fallo consultando a NASA POWER: {}", e.toString());
                return Map.of();
            }
        });

        Condiciones g = deGoogle.join();
        Map<String, NasaPowerClient.ValorDiario> nasa = deNasa.join();
        String medido = g == null ? null : momento(g.currentTime());
        return List.of(
                temperatura(g, medido),
                lluvia(g, medido),
                humedad(g, medido),
                viento(g, medido),
                radiacion(nasa.get(RADIACION), g),
                suelo(nasa.get(SUELO)));
    }

    private static WeatherConditionResponse temperatura(Condiciones g, String medido) {
        if (g == null || g.temperature() == null || g.temperature().degrees() == null) {
            return sinDato("TEMPERATURA_AIRE", "°C", GOOGLE);
        }
        StringJoiner detalle = new StringJoiner(" · ");
        if (g.currentConditionsHistory() != null && g.currentConditionsHistory().temperatureChange() != null
                && g.currentConditionsHistory().temperatureChange().degrees() != null) {
            detalle.add(String.format(ES, "%+.1f °C vs. ayer a esta hora",
                    g.currentConditionsHistory().temperatureChange().degrees()));
        }
        return new WeatherConditionResponse("TEMPERATURA_AIRE", redondear(g.temperature().degrees()),
                "°C", detalle.toString(), GOOGLE, medido);
    }

    private static WeatherConditionResponse lluvia(Condiciones g, String medido) {
        if (g == null || g.currentConditionsHistory() == null || g.currentConditionsHistory().qpf() == null
                || g.currentConditionsHistory().qpf().quantity() == null) {
            return sinDato("PRECIPITACION_24H", "mm", GOOGLE);
        }
        StringJoiner detalle = new StringJoiner(" · ");
        if (g.precipitation() != null) {
            if (g.precipitation().qpf() != null && g.precipitation().qpf().quantity() != null) {
                detalle.add(String.format(ES, "Última hora: %.1f mm", g.precipitation().qpf().quantity()));
            }
            if (g.precipitation().probability() != null && g.precipitation().probability().percent() != null) {
                detalle.add("probabilidad de lluvia " + g.precipitation().probability().percent() + " %");
            }
        }
        return new WeatherConditionResponse("PRECIPITACION_24H",
                redondear(g.currentConditionsHistory().qpf().quantity()), "mm", detalle.toString(), GOOGLE, medido);
    }

    private static WeatherConditionResponse humedad(Condiciones g, String medido) {
        if (g == null || g.relativeHumidity() == null) {
            return sinDato("HUMEDAD_RELATIVA", "%", GOOGLE);
        }
        String detalle = g.dewPoint() == null || g.dewPoint().degrees() == null ? ""
                : String.format(ES, "Punto de rocío: %.1f °C", g.dewPoint().degrees());
        return new WeatherConditionResponse("HUMEDAD_RELATIVA", g.relativeHumidity().doubleValue(),
                "%", detalle, GOOGLE, medido);
    }

    private static WeatherConditionResponse viento(Condiciones g, String medido) {
        if (g == null || g.wind() == null || g.wind().speed() == null || g.wind().speed().value() == null) {
            return sinDato("VIENTO", "km/h", GOOGLE);
        }
        StringJoiner detalle = new StringJoiner(" · ");
        if (g.wind().direction() != null && g.wind().direction().degrees() != null) {
            detalle.add("Dirección " + rumbo(g.wind().direction().degrees()));
        }
        if (g.wind().gust() != null && g.wind().gust().value() != null) {
            detalle.add(String.format(ES, "ráfagas %.0f km/h", g.wind().gust().value()));
        }
        return new WeatherConditionResponse("VIENTO", redondear(g.wind().speed().value()),
                "km/h", detalle.toString(), GOOGLE, medido);
    }

    /** Google no trae radiación solar: sale de NASA POWER (dato diario). El índice UV sí es de Google. */
    private static WeatherConditionResponse radiacion(NasaPowerClient.ValorDiario radiacion, Condiciones g) {
        if (radiacion == null) {
            return sinDato("RADIACION_SOLAR", "kWh/m²", NASA);
        }
        StringJoiner detalle = new StringJoiner(" · ");
        detalle.add("Total del día");
        if (g != null && g.uvIndex() != null) {
            detalle.add("índice UV " + g.uvIndex());
        }
        return new WeatherConditionResponse("RADIACION_SOLAR", redondear(radiacion.valor() / 3.6),
                "kWh/m²", detalle.toString(), NASA, radiacion.fecha().toString());
    }

    /** Google no trae humedad del suelo: sale de NASA POWER (dato diario de modelo). */
    private static WeatherConditionResponse suelo(NasaPowerClient.ValorDiario suelo) {
        if (suelo == null) {
            return sinDato("HUMEDAD_SUELO", "%", NASA);
        }
        return new WeatherConditionResponse("HUMEDAD_SUELO", redondear(suelo.valor() * 100),
                "%", "Humedad en la zona de raíces", NASA, suelo.fecha().toString());
    }

    private static WeatherConditionResponse sinDato(String variable, String unidad, String fuente) {
        return new WeatherConditionResponse(variable, null, unidad, "Sin dato reciente", fuente, null);
    }

    /** La hora de Google viene en UTC; se entrega en hora de Colombia. */
    private static String momento(String utc) {
        try {
            return Instant.parse(utc).atZone(COLOMBIA).toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS).toString();
        } catch (RuntimeException e) {
            return utc;
        }
    }

    /** Grados (0 = norte, en sentido del reloj) a uno de los 8 rumbos. */
    static String rumbo(double grados) {
        String[] rumbos = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
        return rumbos[(int) Math.round(((grados % 360) + 360) % 360 / 45.0) % 8];
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
