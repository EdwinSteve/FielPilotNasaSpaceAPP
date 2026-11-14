package co.surco.identidad.aplicacion;

import co.surco.compartido.error.ErrorDominio;
import co.surco.identidad.dominio.ErroresIdentidad;
import co.surco.identidad.dominio.Puertos.RepositorioSesiones;
import co.surco.identidad.dominio.Puertos.RepositorioUsuarios;
import co.surco.identidad.dominio.SesionRefresco;
import co.surco.identidad.dominio.TokensRefresco;
import co.surco.identidad.dominio.Usuario;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cambia un refresh token por un par nuevo (rotación). Un refresh token ya usado o revocado
 * que vuelve a llegar indica robo: se revoca toda su familia.
 */
@Service
public class RefrescarSesion {

	private final RepositorioSesiones sesiones;
	private final RepositorioUsuarios usuarios;
	private final AbridorSesiones abridor;
	private final Clock reloj;

	public RefrescarSesion(RepositorioSesiones sesiones, RepositorioUsuarios usuarios, AbridorSesiones abridor,
		Clock reloj) {
		this.sesiones = sesiones;
		this.usuarios = usuarios;
		this.abridor = abridor;
		this.reloj = reloj;
	}

	// noRollbackFor: la revocación por robo se guarda aunque la respuesta sea un error
	@Transactional(noRollbackFor = ErrorDominio.class)
	public Tokens ejecutar(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw ErroresIdentidad.refreshInvalido();
		}
		Instant ahora = reloj.instant();
		SesionRefresco sesion = sesiones.porHash(TokensRefresco.hash(refreshToken))
			.orElseThrow(ErroresIdentidad::refreshInvalido);

		if (sesion.fueConsumida()) {
			sesiones.revocarFamilia(sesion.familia(), ahora);
			throw ErroresIdentidad.refreshInvalido();
		}
		if (sesion.estaVencida(ahora)) {
			throw ErroresIdentidad.refreshInvalido();
		}
		Usuario usuario = usuarios.porId(sesion.usuarioId()).orElseThrow(ErroresIdentidad::refreshInvalido);

		sesion.marcarUsada(ahora);
		sesiones.guardar(sesion);
		return abridor.continuar(usuario, sesion.familia(), ahora);
	}
}
