package co.surco.recomendacion.infraestructura.planificador;

import co.surco.compartido.error.ErrorDominio;
import co.surco.recomendacion.dominio.ErroresRecomendacion;
import co.surco.recomendacion.dominio.PlanCiclo;
import co.surco.recomendacion.dominio.Puertos.MotorPlanificador;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Cliente HTTP del planificador estacional (ml/planificador, {@code python -m surco_planificador.servidor}).
 * Usa el mapeador JSON de Spring (snake_case), igual que la API: {@code rend_p50} -> {@code rendP50}.
 *
 * <p>Traduce las respuestas de error del planificador ({@code {"code", "message", "fuente"?}}):
 * <ul>
 *   <li>422 PLANIFICADOR_TERRENO_SIN_SUELO -> RECOMENDACION_TERRENO_SIN_SUELO (422)</li>
 *   <li>422 PLANIFICADOR_MES_NO_ENTRENADO -> RECOMENDACION_MES_NO_ENTRENADO (422)</li>
 *   <li>502 PLANIFICADOR_FUENTE_CAIDA -> RECOMENDACION_FUENTE_CAIDA (502, con la fuente)</li>
 *   <li>otro 4xx -> RECOMENDACION_TERRENO_SIN_DATOS (400)</li>
 *   <li>sin conexión, tiempo agotado o 5xx -> RECOMENDACION_NO_DISPONIBLE (503)</li>
 * </ul>
 */
@Component
class ClientePlanificador implements MotorPlanificador {

	private static final Logger LOG = LoggerFactory.getLogger(ClientePlanificador.class);

	private final RestClient http;
	private final ObjectMapper json;

	ClientePlanificador(RestClient.Builder builder, ObjectMapper json, @Value("${surco.planificador.url}") String url,
		@Value("${surco.planificador.tiempo-maximo:180s}") Duration tiempoMaximo) {
		var fabrica = new SimpleClientHttpRequestFactory();
		fabrica.setConnectTimeout(Duration.ofSeconds(5));
		// La primera consulta de una zona baja NASA POWER y SoilGrids: puede tardar 1-3 minutos
		fabrica.setReadTimeout(tiempoMaximo);
		this.http = builder.baseUrl(url).requestFactory(fabrica).build();
		this.json = json;
	}

	@Override
	public PlanCiclo planificar(double lat, double lon, boolean riego, LocalDate siembra) {
		Map<String, Object> cuerpo = new HashMap<>(Map.of("lat", lat, "lon", lon, "riego", riego));
		if (siembra != null) {
			cuerpo.put("siembra", siembra.toString());
		}
		// El cuerpo va serializado aquí, escrito tal cual (sin convertidores) y con Content-Length: sin eso
		// RestClient lo manda "chunked" y el planificador (http.server de Python) lee un cuerpo vacío
		// ("Envíe lat y lon en grados decimales").
		byte[] json = this.json.writeValueAsBytes(cuerpo);
		try {
			PlanCiclo plan = http.post().uri("/planificar").contentType(MediaType.APPLICATION_JSON)
				.contentLength(json.length).body(salida -> salida.write(json)).retrieve().body(PlanCiclo.class);
			if (plan == null) {
				throw ErroresRecomendacion.modeloNoDisponible();
			}
			return plan;
		} catch (RestClientResponseException e) {
			throw traducir(e.getStatusCode().value(), e.getResponseBodyAsString(), lat, lon);
		} catch (ResourceAccessException e) { // sin conexión o se pasó el tiempo
			LOG.error("El planificador no responde: {}", e.getMessage());
			throw ErroresRecomendacion.modeloNoDisponible();
		} catch (RestClientException e) { // respuesta que no se pudo leer
			LOG.error("Falló el planificador", e);
			throw ErroresRecomendacion.modeloNoDisponible();
		}
	}

	ErrorDominio traducir(int estado, String cuerpo, double lat, double lon) {
		JsonNode error = leer(cuerpo);
		String codigo = error.path("code").asString("");
		LOG.warn("El planificador respondió {} {} para ({}, {}): {}", estado, codigo, lat, lon,
			error.path("message").asString(""));
		return switch (codigo) {
			case "PLANIFICADOR_TERRENO_SIN_SUELO" -> ErroresRecomendacion.terrenoSinSuelo();
			case "PLANIFICADOR_MES_NO_ENTRENADO" -> ErroresRecomendacion.mesNoEntrenado();
			case "PLANIFICADOR_FUENTE_CAIDA" -> ErroresRecomendacion.fuenteCaida(error.path("fuente").asString(null));
			default -> estado >= 400 && estado < 500
				? ErroresRecomendacion.terrenoSinDatos(error.path("message").asString(null))
				: ErroresRecomendacion.modeloNoDisponible();
		};
	}

	private JsonNode leer(String cuerpo) {
		try {
			return json.readTree(cuerpo == null || cuerpo.isBlank() ? "{}" : cuerpo);
		} catch (RuntimeException e) { // el cuerpo no es JSON (un proxy, por ejemplo)
			return json.createObjectNode();
		}
	}
}
