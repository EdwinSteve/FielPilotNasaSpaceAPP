package co.surco;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * API de SURCO. Monolito modular: cada paquete directo de {@code co.surco} es un módulo
 * (identidad, territorio, fincas...) con sus capas dominio, aplicacion, infraestructura y api.
 * Ver docs/arquitectura.md.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SurcoApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SurcoApiApplication.class, args);
	}
}
