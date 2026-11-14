package co.surco.identidad.aplicacion;

import co.surco.compartido.seguridad.PropiedadesJwt;
import co.surco.identidad.dominio.Puertos.EmisorTokensAcceso;
import co.surco.identidad.dominio.Puertos.RepositorioSesiones;
import co.surco.identidad.dominio.SesionRefresco;
import co.surco.identidad.dominio.TokensRefresco;
import co.surco.identidad.dominio.Usuario;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Emite el par access token + refresh token y guarda el hash del refresh token. */
@Component
public class AbridorSesiones {

	private final EmisorTokensAcceso emisor;
	private final RepositorioSesiones sesiones;
	private final PropiedadesJwt propiedades;

	public AbridorSesiones(EmisorTokensAcceso emisor, RepositorioSesiones sesiones, PropiedadesJwt propiedades) {
		this.emisor = emisor;
		this.sesiones = sesiones;
		this.propiedades = propiedades;
	}

	/** Sesión nueva (registro o login): empieza una familia de refresh tokens. */
	public Tokens abrir(Usuario usuario, Instant ahora) {
		return continuar(usuario, UUID.randomUUID(), ahora);
	}

	/** Rotación: el refresh token nuevo pertenece a la misma familia que el que se usó. */
	public Tokens continuar(Usuario usuario, UUID familia, Instant ahora) {
		String refresh = TokensRefresco.generar();
		sesiones.guardar(SesionRefresco.nueva(usuario.id(), familia, TokensRefresco.hash(refresh), ahora,
			ahora.plus(propiedades.duracionRefresco())));
		return new Tokens(emisor.emitir(usuario, ahora), refresh, emisor.duracionSegundos());
	}
}
