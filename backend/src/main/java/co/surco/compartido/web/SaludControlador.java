package co.surco.compartido.web;

import java.time.Clock;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/v1/health}: la API responde. Pública. */
@RestController
public class SaludControlador {

	@GetMapping("/api/v1/health")
	Map<String, String> salud() {
		return Map.of("status", "ok");
	}

	/** El reloj se inyecta (nunca {@code Instant.now()} directo) para poder probar vencimientos y bloqueos. */
	@Configuration
	static class ConfiguracionReloj {
		@Bean
		Clock reloj() {
			return Clock.systemUTC();
		}
	}
}
