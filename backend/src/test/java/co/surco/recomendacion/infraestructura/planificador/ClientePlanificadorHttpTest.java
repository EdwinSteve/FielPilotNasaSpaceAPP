package co.surco.recomendacion.infraestructura.planificador;

import static org.assertj.core.api.Assertions.assertThat;

import co.surco.recomendacion.PlanesDePrueba;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;

/**
 * El cliente contra un servidor HTTP de verdad. El planificador (http.server de Python) lee el cuerpo
 * con Content-Length y no entiende Transfer-Encoding: chunked; si el cliente no manda la longitud, el
 * planificador recibe un cuerpo vacío y responde "Envíe lat y lon en grados decimales".
 */
class ClientePlanificadorHttpTest {

	HttpServer servidor;
	final AtomicReference<String> longitud = new AtomicReference<>();
	final AtomicReference<String> transferencia = new AtomicReference<>();
	final AtomicReference<String> cuerpo = new AtomicReference<>();

	@BeforeEach
	void arrancar() throws Exception {
		byte[] plan;
		try (InputStream in = getClass().getResourceAsStream("/planificador/plan-neiva-bf.json")) {
			plan = in.readAllBytes();
		}
		servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/planificar", e -> {
			longitud.set(e.getRequestHeaders().getFirst("Content-Length"));
			transferencia.set(e.getRequestHeaders().getFirst("Transfer-Encoding"));
			cuerpo.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			e.getResponseHeaders().add("Content-Type", "application/json");
			e.sendResponseHeaders(200, plan.length);
			e.getResponseBody().write(plan);
			e.close();
		});
		servidor.start();
	}

	@AfterEach
	void parar() {
		servidor.stop(0);
	}

	@Test
	void mandaElCuerpoConContentLengthYSinChunked() {
		// Como el RestClient.Builder de Spring en la app: JSON en snake_case
		var builder = RestClient.builder().configureMessageConverters(c -> c.withJsonConverter(
			new JacksonJsonHttpMessageConverter(PlanesDePrueba.JSON)));
		var cliente = new ClientePlanificador(builder, PlanesDePrueba.JSON,
			"http://127.0.0.1:" + servidor.getAddress().getPort(), Duration.ofSeconds(5));

		var plan = cliente.planificar(2.9275, -75.2875, true, LocalDate.of(2026, 12, 1));

		assertThat(plan.motor().id()).isEqualTo("bf");
		assertThat(transferencia.get()).isNull();
		assertThat(longitud.get()).isNotNull();
		assertThat(Integer.parseInt(longitud.get())).isEqualTo(cuerpo.get().getBytes(StandardCharsets.UTF_8).length);
		assertThat(cuerpo.get()).contains("\"lat\":2.9275").contains("\"lon\":-75.2875").contains("\"riego\":true")
			.contains("\"siembra\":\"2026-12-01\"");
	}
}
