package co.surco.identidad.aplicacion;

import co.surco.identidad.dominio.Puertos.RepositorioSesiones;
import co.surco.identidad.dominio.TokensRefresco;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierra sesión revocando los refresh tokens. El access token sigue siendo válido hasta que venza
 * (máximo {@code surco.jwt.duracion-acceso}): por eso se mantiene corto.
 */
@Service
public class CerrarSesion {

	private final RepositorioSesiones sesiones;
	private final Clock reloj;

	public CerrarSesion(RepositorioSesiones sesiones, Clock reloj) {
		this.sesiones = sesiones;
		this.reloj = reloj;
	}

	/** Esta sesión (este dispositivo). Idempotente: un token desconocido no es un error. */
	@Transactional
	public void ejecutar(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			return;
		}
		sesiones.porHash(TokensRefresco.hash(refreshToken))
			.ifPresent(s -> sesiones.revocarFamilia(s.familia(), reloj.instant()));
	}

	/** Todas las sesiones del usuario (por ejemplo, si perdió el celular). */
	@Transactional
	public void todas(UUID usuarioId) {
		sesiones.revocarTodasDelUsuario(usuarioId, reloj.instant());
	}
}
