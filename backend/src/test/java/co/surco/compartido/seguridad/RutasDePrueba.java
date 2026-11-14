package co.surco.compartido.seguridad;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rutas que solo existen en las pruebas. Son el ejemplo de cómo un módulo nuevo protege sus rutas:
 * sin configuración extra queda protegida; con @PreAuthorize exige un rol.
 */
@RestController
class RutasDePrueba {

	@GetMapping("/api/v1/prueba/protegida")
	Map<String, String> protegida(@AuthenticationPrincipal UsuarioAutenticado usuario) {
		return Map.of("usuario", usuario.id().toString(), "rol", usuario.rol());
	}

	@GetMapping("/api/v1/prueba/admin")
	@PreAuthorize("hasRole('ADMIN')")
	Map<String, String> soloAdmin() {
		return Map.of("ok", "true");
	}
}
