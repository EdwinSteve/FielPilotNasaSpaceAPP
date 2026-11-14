package co.surco.identidad.dominio;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Puertos que el dominio de identidad necesita del exterior. La infraestructura los implementa
 * (PostgreSQL, BCrypt, JWT, módulo territorio) y las pruebas los reemplazan por versiones en memoria.
 */
public final class Puertos {

	private Puertos() {
	}

	public interface RepositorioUsuarios {
		Optional<Usuario> porId(UUID id);

		Optional<Usuario> porCelular(Celular celular);

		boolean existeCelular(Celular celular);

		boolean existeCorreo(Correo correo);

		void guardar(Usuario usuario);
	}

	public interface RepositorioSesiones {
		Optional<SesionRefresco> porHash(String hashToken);

		void guardar(SesionRefresco sesion);

		/** Revoca todas las sesiones vigentes de la familia (cierre de sesión o robo detectado). */
		void revocarFamilia(UUID familia, Instant ahora);

		/** Revoca todas las sesiones vigentes del usuario (cerrar sesión en todos los dispositivos). */
		void revocarTodasDelUsuario(UUID usuarioId, Instant ahora);
	}

	public interface RepositorioPoliticas {
		Optional<PoliticaDatos> vigente();

		void registrarConsentimiento(UUID usuarioId, String versionPolitica, Instant ahora);
	}

	public interface CifradorContrasenas {
		String cifrar(String contrasena);

		boolean coincide(String contrasena, String hash);
	}

	/** Emite el access token (JWT) de un usuario. */
	public interface EmisorTokensAcceso {
		String emitir(Usuario usuario, Instant ahora);

		long duracionSegundos();
	}

	/** ¿Existe el municipio? Lo responde el módulo territorio. */
	public interface ValidadorMunicipios {
		boolean existe(String codigoMunicipio);
	}
}
