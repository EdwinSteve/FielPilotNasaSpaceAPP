package co.surco.identidad.infraestructura.seguridad;

import co.surco.identidad.dominio.Puertos.CifradorContrasenas;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt por medio del codificador delegante de Spring: el hash queda como "{bcrypt}$2a$10$...".
 * El prefijo permite cambiar de algoritmo más adelante sin invalidar las contraseñas guardadas.
 */
@Component
class CifradorBCrypt implements CifradorContrasenas {

	private final PasswordEncoder codificador = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	@Override
	public String cifrar(String contrasena) {
		return codificador.encode(contrasena);
	}

	@Override
	public boolean coincide(String contrasena, String hash) {
		return codificador.matches(contrasena, hash);
	}
}
