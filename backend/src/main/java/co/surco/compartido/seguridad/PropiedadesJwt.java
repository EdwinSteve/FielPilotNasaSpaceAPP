package co.surco.compartido.seguridad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración del JWT ({@code surco.jwt.*} en application.yml).
 *
 * @param secreto           clave HMAC de 64+ caracteres; viene de la variable de entorno JWT_SECRETO.
 *                          La app no arranca sin ella.
 * @param emisor            claim {@code iss}
 * @param audiencia         claim {@code aud}
 * @param duracionAcceso    vida del access token (corta: se renueva con el refresh token)
 * @param duracionRefresco  vida del refresh token
 */
@Validated
@ConfigurationProperties("surco.jwt")
public record PropiedadesJwt(
	// Sin anotaciones de validación a propósito: si fallara aquí, Spring imprimiría el valor del secreto
	// en el log. Se valida en ConfiguracionSeguridad.claveJwt, que nunca lo muestra.
	String secreto,
	@NotBlank String emisor,
	@NotBlank String audiencia,
	@NotNull Duration duracionAcceso,
	@NotNull Duration duracionRefresco) {
}
