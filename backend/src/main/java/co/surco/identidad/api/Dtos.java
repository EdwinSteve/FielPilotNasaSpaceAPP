package co.surco.identidad.api;

import co.surco.identidad.aplicacion.Tokens;
import co.surco.identidad.dominio.PoliticaDatos;
import co.surco.identidad.dominio.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Cuerpos JSON de identidad. La API usa snake_case (spring.jackson.property-naming-strategy):
 * {@code nombreCompleto} viaja como {@code nombre_completo}. Las reglas finas (formato del celular,
 * fuerza de la contraseña) las valida el dominio; aquí solo se exige que los campos lleguen.
 */
final class Dtos {

	private Dtos() {
	}

	record SolicitudRegistro(
		@NotBlank(message = "Escriba su nombre y apellido completos.") String nombreCompleto,
		@NotBlank(message = "Escriba su número de celular.") String celular,
		String correo,
		@NotBlank(message = "Escriba una contraseña.") String contrasena,
		@NotBlank(message = "Elija el municipio donde vive.") String municipioResidencia,
		@NotNull(message = "Indique si autoriza el tratamiento de sus datos.") Boolean aceptaPolitica,
		@NotBlank(message = "Falta la versión de la política de datos.") String versionPolitica) {
	}

	record SolicitudIngreso(
		@NotBlank(message = "Escriba su número de celular.") String celular,
		@NotBlank(message = "Escriba su contraseña.") String contrasena) {
	}

	record SolicitudRefresco(@NotBlank(message = "Falta el refresh token.") String refreshToken) {
	}

	/** {@code token_type} siempre "bearer": el access token va en {@code Authorization: Bearer <access_token>}. */
	record RespuestaTokens(String accessToken, String refreshToken, String tokenType, long expiraEn) {
		static RespuestaTokens de(Tokens t) {
			return new RespuestaTokens(t.accessToken(), t.refreshToken(), "bearer", t.expiraEnSegundos());
		}
	}

	record RespuestaUsuario(UUID id, String nombreCompleto, String celular, String correo,
		String municipioResidencia, String departamentoResidencia, String rol, Instant creadoEn) {
		static RespuestaUsuario de(Usuario u) {
			return new RespuestaUsuario(u.id(), u.nombreCompleto(), u.celular().valor(),
				u.correo() == null ? null : u.correo().valor(), u.municipioResidencia(),
				u.municipioResidencia().substring(0, 2), u.rol().name(), u.creadoEn());
		}
	}

	record RespuestaRegistro(RespuestaUsuario usuario, RespuestaTokens tokens) {
	}

	record RespuestaPolitica(String version, String urlTexto, String contenido) {
		static RespuestaPolitica de(PoliticaDatos p) {
			return new RespuestaPolitica(p.version(), p.urlTexto(), p.contenido());
		}
	}
}
