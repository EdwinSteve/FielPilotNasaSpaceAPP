package co.surco.compartido.seguridad;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Respuestas 401 y 403 con el mismo formato {@code {"code", "message"}} que el resto de la API,
 * en vez de la página de error por defecto de Spring Security.
 */
final class RespuestasSeguridad {

	private RespuestasSeguridad() {
	}

	/** 401: falta el token, está vencido, la firma no coincide o el emisor/audiencia no es el nuestro. */
	static AuthenticationEntryPoint noAutenticado() {
		return (peticion, respuesta, error) -> {
			boolean tokenInvalido = error instanceof InvalidBearerTokenException
				|| peticion.getHeader("Authorization") != null;
			// El encabezado WWW-Authenticate sigue el estándar de tokens Bearer (RFC 6750)
			respuesta.setHeader("WWW-Authenticate",
				tokenInvalido ? "Bearer error=\"invalid_token\"" : "Bearer");
			escribir(respuesta, HttpServletResponse.SC_UNAUTHORIZED,
				tokenInvalido ? "AUTH_TOKEN_INVALIDO" : "AUTH_NO_AUTENTICADO",
				tokenInvalido ? "Su sesión venció o no es válida. Vuelva a iniciar sesión."
					: "Debe iniciar sesión para continuar.");
		};
	}

	/** 403: el token es válido pero el rol no alcanza (por ejemplo, @PreAuthorize("hasRole('ADMIN')")). */
	static AccessDeniedHandler sinPermiso() {
		return (peticion, respuesta, error) -> escribir(respuesta, HttpServletResponse.SC_FORBIDDEN,
			"AUTH_SIN_PERMISO", "No tiene permiso para hacer esto.");
	}

	private static void escribir(HttpServletResponse respuesta, int estado, String codigo, String mensaje)
		throws IOException {
		respuesta.setStatus(estado);
		respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
		respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
		// Textos fijos sin comillas ni barras: no necesitan escaparse
		respuesta.getWriter().write("{\"code\":\"" + codigo + "\",\"message\":\"" + mensaje + "\"}");
	}
}
