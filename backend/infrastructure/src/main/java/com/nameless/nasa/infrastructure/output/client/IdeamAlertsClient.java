package com.nameless.nasa.infrastructure.output.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class IdeamAlertsClient {
    private static final String BASE = "https://visualizador.ideam.gov.co/gisserver/rest/services";
    private static final String SERVICIO = "/StoryMaps_IDA/Alertas_Hidrologicas/MapServer";
    private static final String CAPA_AREAS = SERVICIO + "/0/query";    // todo el país
    private static final String CAPA_ALERTAS = SERVICIO + "/2/query";  // solo alertas vigentes

    private final RestClient http;

    public IdeamAlertsClient() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5_000);
        fabrica.setReadTimeout(15_000);
        this.http = RestClient.builder().baseUrl(BASE).requestFactory(fabrica).build();
    }

    /** Vacío si la subzona del punto no tiene alerta vigente. */
    public Optional<Subzona> subzonaEn(double lat, double lon) {
        Respuesta respuesta = http.get()
                .uri(uri -> uri.path(CAPA_ALERTAS)
                        .queryParam("geometry", punto(lat, lon))
                        .queryParam("geometryType", "esriGeometryPoint")
                        .queryParam("inSR", 4326)
                        .queryParam("spatialRel", "esriSpatialRelIntersects")
                        .queryParam("outFields", "NIVEL_A,NOMSZH")
                        .queryParam("returnGeometry", false)
                        .queryParam("f", "json")
                        .build())
                .retrieve()
                .body(Respuesta.class);

        // ArcGIS responde HTTP 200 con {"error": {...}} cuando la consulta falla.
        if (respuesta == null || respuesta.error() != null || respuesta.features() == null) {
            throw new IllegalStateException("El IDEAM respondió con un error");
        }
        return respuesta.features().stream().map(Feature::attributes).findFirst();
    }

    /** true si el punto cae en alguna área hidrográfica, es decir, dentro de Colombia. */
    public boolean estaEnColombia(double lat, double lon) {
        Conteo conteo = http.get()
                .uri(uri -> uri.path(CAPA_AREAS)
                        .queryParam("geometry", punto(lat, lon))
                        .queryParam("geometryType", "esriGeometryPoint")
                        .queryParam("inSR", 4326)
                        .queryParam("spatialRel", "esriSpatialRelIntersects")
                        .queryParam("returnCountOnly", true)
                        .queryParam("f", "json")
                        .build())
                .retrieve()
                .body(Conteo.class);

        if (conteo == null || conteo.error() != null || conteo.count() == null) {
            throw new IllegalStateException("El IDEAM respondió con un error");
        }
        return conteo.count() > 0;
    }

    // ArcGIS espera "x,y", es decir longitud primero. Locale.ROOT evita la coma decimal
    // y la notación científica (latitudes cerca del ecuador, como 0.0001).
    private static String punto(double lat, double lon) {
        return String.format(Locale.ROOT, "%.6f,%.6f", lon, lat);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Respuesta(List<Feature> features, ErrorArcGis error) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feature(Subzona attributes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Subzona(
            @JsonProperty("NIVEL_A") String nivel,
            @JsonProperty("NOMSZH") String nombre) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Conteo(Integer count, ErrorArcGis error) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorArcGis(int code, String message) {}
}