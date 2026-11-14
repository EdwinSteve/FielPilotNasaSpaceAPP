package co.surco.compartido.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Cuerpo de todas las respuestas de error de la API: {@code {"code", "message", "details"?}}. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record RespuestaError(String code, String message, List<ErrorCampo> details) {

	public static RespuestaError de(String code, String message) {
		return new RespuestaError(code, message, List.of());
	}
}
