package co.surco.identidad.aplicacion;

import co.surco.compartido.error.ErrorDominio;
import co.surco.identidad.dominio.Celular;
import co.surco.identidad.dominio.ErroresIdentidad;
import co.surco.identidad.dominio.Puertos.CifradorContrasenas;
import co.surco.identidad.dominio.Puertos.RepositorioUsuarios;
import co.surco.identidad.dominio.Usuario;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicio de sesión con celular y contraseña.
 *
 * <ul>
 *   <li>Mismo error si el celular no existe o la contraseña está mal (no revela qué celulares tienen cuenta).</li>
 *   <li>Con un celular inexistente igual se compara contra un hash señuelo, para que el tiempo de respuesta
 *       no delate si la cuenta existe.</li>
 *   <li>Tras {@code maximoIntentos} fallos seguidos la cuenta se bloquea {@code duracionBloqueo} (423).</li>
 * </ul>
 */
@Service
public class IniciarSesion {

	public record Comando(String celular, String contrasena) {
	}

	private final RepositorioUsuarios usuarios;
	private final CifradorContrasenas cifrador;
	private final AbridorSesiones sesiones;
	private final PropiedadesIngreso reglas;
	private final Clock reloj;
	private final String hashSenuelo;

	public IniciarSesion(RepositorioUsuarios usuarios, CifradorContrasenas cifrador, AbridorSesiones sesiones,
		PropiedadesIngreso reglas, Clock reloj) {
		this.usuarios = usuarios;
		this.cifrador = cifrador;
		this.sesiones = sesiones;
		this.reglas = reglas;
		this.reloj = reloj;
		this.hashSenuelo = cifrador.cifrar("senuelo-para-igualar-tiempos-1");
	}

	// noRollbackFor: el intento fallido se guarda aunque la respuesta sea un error
	@Transactional(noRollbackFor = ErrorDominio.class)
	public Tokens ejecutar(Comando c) {
		Instant ahora = reloj.instant();
		Optional<Usuario> encontrado = buscar(c.celular());
		String contrasena = c.contrasena() == null ? "" : c.contrasena();

		if (encontrado.isEmpty()) {
			cifrador.coincide(contrasena, hashSenuelo);
			throw ErroresIdentidad.credencialesInvalidas();
		}
		Usuario usuario = encontrado.get();
		if (usuario.estaBloqueado(ahora)) {
			throw ErroresIdentidad.cuentaBloqueada(minutosRestantes(usuario, ahora));
		}
		if (!cifrador.coincide(contrasena, usuario.contrasenaHash())) {
			usuario.registrarIntentoFallido(ahora, reglas.maximoIntentos(), reglas.duracionBloqueo());
			usuarios.guardar(usuario);
			if (usuario.estaBloqueado(ahora)) {
				throw ErroresIdentidad.cuentaBloqueada(minutosRestantes(usuario, ahora));
			}
			throw ErroresIdentidad.credencialesInvalidas();
		}

		usuario.registrarIngresoExitoso();
		usuarios.guardar(usuario);
		return sesiones.abrir(usuario, ahora);
	}

	private Optional<Usuario> buscar(String celular) {
		try {
			return usuarios.porCelular(Celular.de(celular));
		} catch (ErrorDominio formatoInvalido) {
			return Optional.empty();
		}
	}

	private static long minutosRestantes(Usuario usuario, Instant ahora) {
		long segundos = Duration.between(ahora, usuario.bloqueadoHasta()).toSeconds();
		return Math.max(1, (segundos + 59) / 60);
	}
}
