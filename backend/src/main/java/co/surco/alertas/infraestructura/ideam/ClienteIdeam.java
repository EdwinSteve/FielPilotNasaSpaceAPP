package co.surco.alertas.infraestructura.ideam;

import co.surco.alertas.dominio.ErroresAlertas;
import co.surco.alertas.dominio.Puertos.FuenteAlertasRios;
import co.surco.alertas.dominio.Puertos.SubzonaConAlerta;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Servidor GIS del IDEAM (API REST de ArcGIS), servicio StoryMaps_IDA/Alertas_Hidrologicas:
 * capa 2 (Alertas_vigentes) y capa 0 (áreas hidrográficas de todo el país).
 * Su certificado necesita {@code com.sun.security.enableAIAcaIssuers} (ver SurcoApiApplication).
 */
@Component
class ClienteIdeam implements FuenteAlertasRios {

	private static final Logger LOG = LoggerFactory.getLogger(ClienteIdeam.class);
	private static final String BASE = "https://visualizador.ideam.gov.co/gisserver/rest/services";
	private static final String SERVICIO = "/StoryMaps_IDA/Alertas_Hidrologicas/MapServer";
	private static final String CAPA_AREAS = SERVICIO + "/0/query";
	private static final String CAPA_ALERTAS = SERVICIO + "/2/query";

	private final RestClient http;

	ClienteIdeam(RestClient.Builder builder) {
		var fabrica = new SimpleClientHttpRequestFactory();
		fabrica.setConnectTimeout(Duration.ofSeconds(5));
		fabrica.setReadTimeout(Duration.ofSeconds(15));
		this.http = builder.baseUrl(BASE).requestFactory(fabrica).build();
	}

	@Override
	public Optional<SubzonaConAlerta> conAlerta(double lat, double lon) {
		Respuesta r = consultar(CAPA_ALERTAS, lat, lon, false, Respuesta.class);
		// ArcGIS responde HTTP 200 con {"error": {...}} cuando la consulta falla
		if (r == null || r.error() != null || r.features() == null) {
			throw noDisponible("respondió un error a la consulta de alertas");
		}
		return r.features().stream().map(Feature::attributes)
			.map(a -> new SubzonaConAlerta(a.nivel(), a.nombre())).findFirst();
	}

	@Override
	public boolean dentroDeColombia(double lat, double lon) {
		Conteo c = consultar(CAPA_AREAS, lat, lon, true, Conteo.class);
		if (c == null || c.error() != null || c.count() == null) {
			throw noDisponible("respondió un error a la consulta de áreas");
		}
		return c.count() > 0;
	}

	private <T> T consultar(String capa, double lat, double lon, boolean soloConteo, Class<T> tipo) {
		try {
			return http.get().uri(uri -> {
				uri.path(capa)
					// ArcGIS espera "x,y" (longitud primero); Locale.ROOT evita la coma decimal
					.queryParam("geometry", String.format(Locale.ROOT, "%.6f,%.6f", lon, lat))
					.queryParam("geometryType", "esriGeometryPoint")
					.queryParam("inSR", 4326)
					.queryParam("spatialRel", "esriSpatialRelIntersects")
					.queryParam("returnGeometry", false)
					.queryParam("f", "json");
				if (soloConteo) {
					uri.queryParam("returnCountOnly", true);
				} else {
					uri.queryParam("outFields", "NIVEL_A,NOMSZH");
				}
				return uri.build();
			}).retrieve().body(tipo);
		} catch (RestClientException e) {
			LOG.error("El IDEAM no responde", e);
			throw ErroresAlertas.fuenteNoDisponible("al IDEAM");
		}
	}

	private static RuntimeException noDisponible(String detalle) {
		LOG.error("El IDEAM {}", detalle);
		return ErroresAlertas.fuenteNoDisponible("al IDEAM");
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Respuesta(List<Feature> features, ErrorArcGis error) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Feature(Atributos attributes) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Atributos(@JsonProperty("NIVEL_A") String nivel, @JsonProperty("NOMSZH") String nombre) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Conteo(Integer count, ErrorArcGis error) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record ErrorArcGis(Integer code, String message) {
	}
}
