package co.surco.identidad.aplicacion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bloqueo por intentos fallidos ({@code surco.ingreso.*}): tras {@code maximoIntentos} contraseñas
 * equivocadas seguidas, la cuenta queda bloqueada {@code duracionBloqueo}.
 */
@Validated
@ConfigurationProperties("surco.ingreso")
public record PropiedadesIngreso(@Min(1) int maximoIntentos, @NotNull Duration duracionBloqueo) {
}
