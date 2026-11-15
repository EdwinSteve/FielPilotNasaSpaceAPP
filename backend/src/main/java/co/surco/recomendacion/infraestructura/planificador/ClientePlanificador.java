package co.surco.recomendacion.infraestructura.planificador;

import co.surco.recomendacion.dominio.ErroresRecomendacion;
import co.surco.recomendacion.dominio.PlanCiclo;
import co.surco.recomendacion.dominio.Puertos.MotorPlanificador;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente HTTP del planificador estacional (ml/planificador, {@code python -m surco_planificador.servidor}).
 * Usa el mapeador JSON de Spring (snake_case), igual que la API: {@code rend_p50} -> {@code rendP50}.
 */
@Component
class ClientePlanificador implements MotorPlanificador {

	private static final Logger LOG = LoggerFactory.getLogger(ClientePlanificador.class);

	private final RestClient http;

	ClientePlanificador(RestClient.Builder builder, @Value("${surco.planificador.url}") String url,
		@Value("${surco.planificador.tiempo-maximo:180s}") Duration tiempoMaximo) {
		var fabrica = new SimpleClientHttpRequestFactory();
		fabrica.setConnectTimeout(Duration.ofSeconds(5));
		// La primera consulta de una zona baja NASA POWER y SoilGrids: puede tardar 1-2 minutos
		fabrica.setReadTimeout(tiempoMaximo);
		this.http = builder.baseUrl(url).requestFactory(fabrica).build();
	}

	@Override
	public PlanCiclo planificar(double lat, double lon, boolean riego) {
		try {
			PlanCiclo plan = http.post().uri("/planificar").contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("lat", lat, "lon", lon, "riego", riego)).retrieve().body(PlanCiclo.class);
			if (plan == null) {
				throw ErroresRecomendacion.modeloNoDisponible();
			}
			return plan;
		} catch (HttpClientErrorException e) { // 400: el terreno no tiene datos suficientes
			String detalle = e.getResponseBodyAsString();
			LOG.warn("El planificador rechazó ({}, {}): {}", lat, lon, detalle);
			throw ErroresRecomendacion.terrenoSinDatos(mensajeDe(detalle));
		} catch (ResourceAccessException e) { // sin conexión o se pasó el tiempo
			LOG.error("El planificador no responde: {}", e.getMessage());
			throw ErroresRecomendacion.modeloNoDisponible();
		} catch (RestClientException e) { // 500 u otra respuesta que no se pudo leer
			LOG.error("Falló el planificador", e);
			throw ErroresRecomendacion.modeloNoDisponible();
		}
	}

	/** El "message" del cuerpo {"code", "message"} del planificador, sin depender de un mapeador JSON. */
	static String mensajeDe(String cuerpo) {
		int i = cuerpo == null ? -1 : cuerpo.indexOf("\"message\"");
		if (i < 0) {
			return null;
		}
		int inicio = cuerpo.indexOf('"', cuerpo.indexOf(':', i) + 1);
		int fin = cuerpo.indexOf('"', inicio + 1);
		return inicio < 0 || fin < 0 ? null : cuerpo.substring(inicio + 1, fin);
	}
}
