package com.nameless.nasa.infrastructure.output.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cliente de NASA POWER para lluvia (parámetro PRECTOTCORR, en mm/día).
 * Hace dos consultas pequeñas en vez de bajar décadas de serie diaria:
 * los últimos días y la climatología mensual ya calculada por la NASA.
 */
@Component
public class NasaPowerClient {
    private static final String BASE = "https://power.larc.nasa.gov/api/temporal";
    private static final String LLUVIA = "PRECTOTCORR";
    private static final double SIN_DATO = -999.0;
    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter AAAAMMDD = DateTimeFormatter.BASIC_ISO_DATE;
    private static final List<String> MESES = List.of(
            "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");

    /** Se piden más de 30 días porque los más recientes suelen llegar sin dato. */
    private static final int DIAS_A_PEDIR = 45;
    /** Si el último dato es más viejo que esto, no sirve para avisar hoy. */
    private static final int DIAS_MAXIMOS_DE_RETRASO = 10;

    private final RestClient http;

    /** Una consulta por zona y por día: la segunda del día no sale a la red. */
    private final Map<String, EnCache> cache = new ConcurrentHashMap<>();
    /** La lluvia normal no cambia: se guarda una sola vez por zona. */
    private final Map<String, Map<Month, Double>> normales = new ConcurrentHashMap<>();

    private record EnCache(LocalDate dia, Lluvia lluvia) {}

    /** Lluvia de los últimos días (mm por día) y lluvia normal de cada mes (mm/día). */
    public record Lluvia(SortedMap<LocalDate, Double> diaria, Map<Month, Double> normalPorMes) {}

    public NasaPowerClient() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(20_000);
        this.http = RestClient.builder().baseUrl(BASE).requestFactory(fabrica).build();
    }

    /**
     * Lluvia reciente y lluvia normal del punto. Las dos consultas salen al mismo tiempo
     * y el resultado se guarda por zona y por día.
     *
     * @throws IllegalStateException si la NASA responde sin datos o con datos atrasados
     */
    public Lluvia lluviaEn(double lat, double lon) {
        LocalDate hoy = LocalDate.now(COLOMBIA);
        // La celda de NASA POWER mide unos 50 km: redondear a 0.1° (unos 11 km)
        // no cambia el resultado y deja que lotes vecinos compartan la caché.
        double latZona = Math.round(lat * 10) / 10.0;
        double lonZona = Math.round(lon * 10) / 10.0;
        String clave = String.format(Locale.ROOT, "%.1f,%.1f", latZona, lonZona);

        EnCache guardado = cache.get(clave);
        if (guardado != null && guardado.dia().equals(hoy)) {
            return guardado.lluvia();
        }

        CompletableFuture<SortedMap<LocalDate, Double>> diaria = CompletableFuture.supplyAsync(
                () -> lluviaDiaria(latZona, lonZona, hoy.minusDays(DIAS_A_PEDIR), hoy));
        CompletableFuture<Map<Month, Double>> normal = CompletableFuture.supplyAsync(
                () -> normales.computeIfAbsent(clave, c -> lluviaNormalPorMes(latZona, lonZona)));
        Lluvia lluvia;
        try {
            lluvia = new Lluvia(diaria.join(), normal.join());
        } catch (CompletionException e) {
            // Se devuelve el error original para que el controlador lo trate como cualquier otro.
            if (e.getCause() instanceof RuntimeException causa) {
                throw causa;
            }
            throw e;
        }

        if (lluvia.diaria().isEmpty()
                || lluvia.diaria().lastKey().isBefore(hoy.minusDays(DIAS_MAXIMOS_DE_RETRASO))) {
            // No se guarda en caché: la próxima petición vuelve a consultar.
            throw new IllegalStateException("NASA POWER no tiene datos de lluvia recientes para " + clave);
        }
        cache.put(clave, new EnCache(hoy, lluvia));
        return lluvia;
    }

    /** Lluvia en mm por día. Los días sin dato (-999) no se incluyen. */
    public SortedMap<LocalDate, Double> lluviaDiaria(
            double lat, double lon, LocalDate inicio, LocalDate fin) {
        Map<String, Double> crudo = consultar("/daily/point", lat, lon, inicio, fin);
        SortedMap<LocalDate, Double> lluvia = new TreeMap<>();
        crudo.forEach((fecha, mm) -> {
            if (mm != null && mm > SIN_DATO) {
                lluvia.put(LocalDate.parse(fecha, AAAAMMDD), mm);
            }
        });
        return lluvia;
    }

    /** Lluvia normal de cada mes en mm/día (climatología de la NASA para ese punto). */
    public Map<Month, Double> lluviaNormalPorMes(double lat, double lon) {
        Map<String, Double> crudo = consultar("/climatology/point", lat, lon, null, null);
        Map<Month, Double> normal = new EnumMap<>(Month.class);
        for (int i = 0; i < MESES.size(); i++) {
            Double mm = crudo.get(MESES.get(i));
            if (mm != null && mm > SIN_DATO) {
                normal.put(Month.of(i + 1), mm);
            }
        }
        return normal;
    }

    /** Un valor diario y el día al que corresponde. */
    public record ValorDiario(LocalDate fecha, double valor) {}

    /**
     * Último valor diario disponible de cada parámetro pedido (por ejemplo radiación o humedad del suelo).
     * Los días más recientes suelen llegar sin dato; un parámetro sin datos en 15 días no aparece.
     */
    public Map<String, ValorDiario> ultimosValores(double lat, double lon, String... parametros) {
        LocalDate hoy = LocalDate.now(COLOMBIA);
        Respuesta respuesta = http.get()
                .uri(uri -> uri.path("/daily/point")
                        .queryParam("parameters", String.join(",", parametros))
                        .queryParam("community", "AG")
                        .queryParam("latitude", String.format(Locale.ROOT, "%.4f", lat))
                        .queryParam("longitude", String.format(Locale.ROOT, "%.4f", lon))
                        .queryParam("start", hoy.minusDays(15).format(AAAAMMDD))
                        .queryParam("end", hoy.format(AAAAMMDD))
                        .queryParam("format", "JSON")
                        .build())
                .retrieve()
                .body(Respuesta.class);

        if (respuesta == null || respuesta.properties() == null
                || respuesta.properties().parameter() == null) {
            throw new IllegalStateException("NASA POWER respondió sin datos diarios");
        }
        Map<String, ValorDiario> ultimos = new HashMap<>();
        respuesta.properties().parameter().forEach((parametro, serie) -> {
            if (serie == null) {
                return;
            }
            // Las fechas vienen como AAAAMMDD: el orden de texto es el orden cronológico.
            serie.entrySet().stream()
                    .filter(dia -> dia.getValue() != null && dia.getValue() > SIN_DATO)
                    .max(Map.Entry.comparingByKey())
                    .ifPresent(dia -> ultimos.put(parametro,
                            new ValorDiario(LocalDate.parse(dia.getKey(), AAAAMMDD), dia.getValue())));
        });
        return ultimos;
    }

    private Map<String, Double> consultar(
            String ruta, double lat, double lon, LocalDate inicio, LocalDate fin) {
        Respuesta respuesta = http.get()
                .uri(uri -> {
                    uri.path(ruta)
                            .queryParam("parameters", LLUVIA)
                            .queryParam("community", "AG")
                            .queryParam("latitude", String.format(Locale.ROOT, "%.4f", lat))
                            .queryParam("longitude", String.format(Locale.ROOT, "%.4f", lon))
                            .queryParam("format", "JSON");
                    if (inicio != null && fin != null) {
                        uri.queryParam("start", inicio.format(AAAAMMDD))
                                .queryParam("end", fin.format(AAAAMMDD));
                    }
                    return uri.build();
                })
                .retrieve()
                .body(Respuesta.class);

        if (respuesta == null || respuesta.properties() == null
                || respuesta.properties().parameter() == null
                || respuesta.properties().parameter().get(LLUVIA) == null) {
            throw new IllegalStateException("NASA POWER respondió sin datos de lluvia en " + ruta);
        }
        return respuesta.properties().parameter().get(LLUVIA);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Respuesta(Propiedades properties) {}

    /** parameter = { "PRECTOTCORR": { "20261101": 3.2, ... } } o { "JAN": 2.1, ... }. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Propiedades(Map<String, Map<String, Double>> parameter) {}
}
