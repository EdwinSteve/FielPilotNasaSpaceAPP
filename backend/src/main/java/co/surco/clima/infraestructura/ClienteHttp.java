package co.surco.clima.infraestructura;

import java.net.URI;
import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** GET que devuelve el JSON como árbol: los servicios externos no usan el snake_case de nuestra API. */
final class ClienteHttp {

	private final RestClient http;
	private final ObjectMapper json;

	ClienteHttp(ObjectMapper json, Duration tiempoMaximo) {
		var fabrica = new SimpleClientHttpRequestFactory();
		fabrica.setConnectTimeout(Duration.ofSeconds(5));
		fabrica.setReadTimeout(tiempoMaximo);
		this.http = RestClient.builder().requestFactory(fabrica).build();
		this.json = json;
	}

	/** @param url ya codificada. Lanza una excepción si el servicio no responde 2xx o no manda JSON. */
	JsonNode get(String url) {
		String cuerpo = http.get().uri(URI.create(url)).retrieve().body(String.class);
		if (cuerpo == null || cuerpo.isBlank()) {
			throw new IllegalStateException("Respuesta vacía de " + URI.create(url).getHost());
		}
		return json.readTree(cuerpo);
	}

	/** {@code null} si el campo no viene o viene en null. */
	static Double numero(JsonNode nodo) {
		return nodo == null || nodo.isMissingNode() || nodo.isNull() ? null : nodo.asDouble();
	}
}
