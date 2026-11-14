package co.surco.identidad.api;

import co.surco.compartido.seguridad.UsuarioAutenticado;
import co.surco.identidad.api.Dtos.RespuestaPolitica;
import co.surco.identidad.api.Dtos.RespuestaUsuario;
import co.surco.identidad.aplicacion.ConsultasIdentidad;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Perfil del usuario autenticado y política de datos.
 *
 * <p>{@code GET /usuarios/me} es el ejemplo de ruta protegida: no tiene nada especial, queda protegida
 * porque no está en RutasPublicas, y recibe el usuario del token con {@code @AuthenticationPrincipal}.
 */
@RestController
class UsuarioControlador {

	private final ConsultasIdentidad consultas;

	UsuarioControlador(ConsultasIdentidad consultas) {
		this.consultas = consultas;
	}

	@GetMapping("/api/v1/usuarios/me")
	RespuestaUsuario yo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
		return RespuestaUsuario.de(consultas.perfil(usuario.id()));
	}

	/** Pública: el formulario de registro la muestra antes de tener cuenta. */
	@GetMapping("/api/v1/politicas/vigente")
	RespuestaPolitica politicaVigente() {
		return RespuestaPolitica.de(consultas.politicaVigente());
	}
}
