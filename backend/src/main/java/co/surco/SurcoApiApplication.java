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
		// El servidor del IDEAM (alertas) no envía la cadena completa de su certificado: así Java descarga
		// el certificado intermedio que le falta en vez de fallar con PKIX path building failed.
		System.setProperty("com.sun.security.enableAIAcaIssuers", "true");
		SpringApplication.run(SurcoApiApplication.class, args);
	}
}
