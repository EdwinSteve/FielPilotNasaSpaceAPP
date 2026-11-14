package co.surco.identidad.api;

import co.surco.compartido.seguridad.UsuarioAutenticado;
import co.surco.identidad.api.Dtos.RespuestaRegistro;
import co.surco.identidad.api.Dtos.RespuestaTokens;
import co.surco.identidad.api.Dtos.RespuestaUsuario;
import co.surco.identidad.api.Dtos.SolicitudIngreso;
import co.surco.identidad.api.Dtos.SolicitudRefresco;
import co.surco.identidad.api.Dtos.SolicitudRegistro;
import co.surco.identidad.aplicacion.CerrarSesion;
import co.surco.identidad.aplicacion.IniciarSesion;
import co.surco.identidad.aplicacion.RefrescarSesion;
import co.surco.identidad.aplicacion.RegistrarUsuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro y sesión. Públicas (ver RutasPublicas) salvo {@code logout-todo}, que exige token.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AutenticacionControlador {

	private final RegistrarUsuario registrar;
	private final IniciarSesion iniciar;
	private final RefrescarSesion refrescar;
	private final CerrarSesion cerrar;

	AutenticacionControlador(RegistrarUsuario registrar, IniciarSesion iniciar, RefrescarSesion refrescar,
		CerrarSesion cerrar) {
		this.registrar = registrar;
		this.iniciar = iniciar;
		this.refrescar = refrescar;
		this.cerrar = cerrar;
	}

	/** 201 con el usuario y sus tokens: queda con la sesión iniciada. */
	@PostMapping("/registro")
	@ResponseStatus(HttpStatus.CREATED)
	RespuestaRegistro registro(@Valid @RequestBody SolicitudRegistro s) {
		var resultado = registrar.ejecutar(new RegistrarUsuario.Comando(s.nombreCompleto(), s.celular(), s.correo(),
			s.contrasena(), s.municipioResidencia(), s.aceptaPolitica(), s.versionPolitica()));
		return new RespuestaRegistro(RespuestaUsuario.de(resultado.usuario()), RespuestaTokens.de(resultado.tokens()));
	}

	/** 200 tokens · 401 AUTH_CREDENCIALES_INVALIDAS · 423 AUTH_CUENTA_BLOQUEADA. */
	@PostMapping("/login")
	RespuestaTokens login(@Valid @RequestBody SolicitudIngreso s) {
		return RespuestaTokens.de(iniciar.ejecutar(new IniciarSesion.Comando(s.celular(), s.contrasena())));
	}

	/** 200 tokens nuevos; el refresh token enviado deja de servir · 401 AUTH_REFRESH_INVALIDO. */
	@PostMapping("/refresh")
	RespuestaTokens refresh(@Valid @RequestBody SolicitudRefresco s) {
		return RespuestaTokens.de(refrescar.ejecutar(s.refreshToken()));
	}

	/** 204. Pública: con el access token vencido el usuario igual debe poder salir. */
	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logout(@Valid @RequestBody SolicitudRefresco s) {
		cerrar.ejecutar(s.refreshToken());
	}

	/** 204. Cierra la sesión en todos los dispositivos. Exige token. */
	@PostMapping("/logout-todo")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void logoutTodo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
		cerrar.todas(usuario.id());
	}
}
