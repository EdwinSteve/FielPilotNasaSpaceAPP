package co.surco.clima.infraestructura;

import co.surco.clima.dominio.Puertos.AlertaRio;
import co.surco.clima.dominio.Puertos.FuenteAlertasRios;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Alertas hidrológicas del visualizador del IDEAM (ArcGIS REST). La capa 2 solo trae las subzonas
 * hidrográficas con alerta vigente: si el punto no cae en ninguna, no hay alerta.
 */
@Component
class IdeamAlertasCliente implements FuenteAlertasRios {

	// ArcGIS espera "x,y": la longitud primero
	private static final String URL = "https://visualizador.ideam.gov.co/gisserver/rest/services/StoryMaps_IDA/"
		+ "Alertas_Hidrologicas/MapServer/2/query?geometry=%.6f,%.6f&geometryType=esriGeometryPoint&inSR=4326"
		+ "&spatialRel=esriSpatialRelIntersects&outFields=NIVEL_A,NOMSZH&returnGeometry=false&f=json";

	private final ClienteHttp http;

	IdeamAlertasCliente(ObjectMapper json) {
		this.http = new ClienteHttp(json, Duration.ofSeconds(12));
	}

	@Override
	public Optional<AlertaRio> vigenteEn(double lat, double lon) {
		JsonNode raiz = http.get(String.format(Locale.ROOT, URL, lon, lat));
		// ArcGIS responde HTTP 200 con {"error": {...}} cuando la consulta falla
		if (raiz.has("error") || !raiz.path("features").isArray()) {
			throw new IllegalStateException("El IDEAM respondió con un error");
		}
		JsonNode features = raiz.path("features");
		if (features.isEmpty()) {
			return Optional.empty();
		}
		JsonNode atributos = features.get(0).path("attributes");
		return Optional.of(new AlertaRio(atributos.path("NIVEL_A").asString(), atributos.path("NOMSZH").asString()));
	}
}
