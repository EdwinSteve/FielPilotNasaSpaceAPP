package co.surco.compartido.seguridad;

import org.springframework.http.HttpMethod;

/**
 * Las ÚNICAS rutas que se pueden llamar sin token. Todo lo demás exige un JWT válido
 * (seguro por defecto): una ruta nueva queda protegida sin hacer nada.
 *
 * <p>Para abrir una ruta nueva, agréguela aquí y explique en el PR por qué no necesita sesión.
 */
public final class RutasPublicas {

	private RutasPublicas() {
	}

	public record Ruta(HttpMethod metodo, String patron) {
	}

	public static final Ruta[] RUTAS = {
		// identidad: crear cuenta y manejar la sesión con el refresh token
		new Ruta(HttpMethod.POST, "/api/v1/auth/registro"),
		new Ruta(HttpMethod.POST, "/api/v1/auth/login"),
		new Ruta(HttpMethod.POST, "/api/v1/auth/refresh"),
		new Ruta(HttpMethod.POST, "/api/v1/auth/logout"),
		new Ruta(HttpMethod.GET, "/api/v1/politicas/vigente"),
		// territorio: el formulario de registro necesita departamentos y municipios antes de tener cuenta
		new Ruta(HttpMethod.GET, "/api/v1/territorio/**"),
		// estado del servicio
		new Ruta(HttpMethod.GET, "/api/v1/health"),
		new Ruta(HttpMethod.GET, "/actuator/health"),
	};
}
