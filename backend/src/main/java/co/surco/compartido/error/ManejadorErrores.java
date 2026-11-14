package co.surco.compartido.error;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduce las excepciones a {@code {"code", "message", "details"?}} con el código HTTP correcto.
 * Ningún error interno (trazas, SQL) llega al cliente.
 */
@RestControllerAdvice
public class ManejadorErrores {

	private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

	@ExceptionHandler(ErrorDominio.class)
	ResponseEntity<RespuestaError> errorDominio(ErrorDominio e) {
		return ResponseEntity.status(estado(e.tipo()))
			.body(new RespuestaError(e.codigo(), e.getMessage(), e.detalles()));
	}

	/** Fallos de @Valid en el cuerpo: 422 con el error de cada campo, con su nombre del JSON. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<RespuestaError> validacion(MethodArgumentNotValidException e) {
		List<ErrorCampo> detalles = e.getBindingResult().getFieldErrors().stream()
			.map(f -> new ErrorCampo(NombresJson.aSnakeCase(f.getField()), f.getDefaultMessage()))
			.toList();
		return ResponseEntity.unprocessableContent()
			.body(new RespuestaError("VALIDATION", "Los datos enviados no son válidos.", detalles));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<RespuestaError> jsonInvalido() {
		return ResponseEntity.badRequest()
			.body(RespuestaError.de("JSON_INVALIDO", "El cuerpo de la petición no es un JSON válido."));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ResponseEntity<RespuestaError> parametroFaltante(MissingServletRequestParameterException e) {
		String campo = e.getParameterName();
		String mensaje = "Falta el parámetro " + campo + ".";
		return ResponseEntity.badRequest()
			.body(new RespuestaError("PARAMETRO_REQUERIDO", mensaje, List.of(new ErrorCampo(campo, mensaje))));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<RespuestaError> rutaInexistente() {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(RespuestaError.de("RUTA_NO_ENCONTRADA", "La ruta no existe."));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<RespuestaError> metodoNoPermitido() {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
			.body(RespuestaError.de("METODO_NO_PERMITIDO", "Este método no está permitido en esta ruta."));
	}

	/**
	 * {@code @PreAuthorize} rechaza dentro del controlador, así que la excepción llega aquí y no al filtro
	 * de seguridad. Sin este manejador caería en {@link #inesperado} y respondería 500.
	 */
	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<RespuestaError> sinPermiso() {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
			.body(RespuestaError.de("AUTH_SIN_PERMISO", "No tiene permiso para hacer esto."));
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<RespuestaError> noAutenticado() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(RespuestaError.de("AUTH_NO_AUTENTICADO", "Debe iniciar sesión para continuar."));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<RespuestaError> inesperado(Exception e) {
		LOG.error("Error no controlado", e);
		return ResponseEntity.internalServerError()
			.body(RespuestaError.de("ERROR_INTERNO", "Ocurrió un error inesperado. Inténtelo de nuevo."));
	}

	static HttpStatus estado(TipoError tipo) {
		return switch (tipo) {
			case SOLICITUD_INVALIDA -> HttpStatus.BAD_REQUEST;
			case NO_AUTENTICADO -> HttpStatus.UNAUTHORIZED;
			case PROHIBIDO -> HttpStatus.FORBIDDEN;
			case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
			case CONFLICTO -> HttpStatus.CONFLICT;
			case BLOQUEADO -> HttpStatus.LOCKED;
		};
	}
}
