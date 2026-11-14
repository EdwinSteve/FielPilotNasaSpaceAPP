package co.surco.compartido.seguridad;

import java.util.UUID;

/**
 * Quién hace la petición, sacado del JWT ya validado. Es lo que reciben los controladores protegidos:
 *
 * <pre>{@code
 * @GetMapping("/api/v1/fincas")
 * List<FincaRespuesta> misFincas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
 *     return listarFincas.de(usuario.id());
 * }
 * }</pre>
 *
 * @param id  id del usuario (claim {@code sub})
 * @param rol rol del usuario (claim {@code rol}): PRODUCTOR o ADMIN
 */
public record UsuarioAutenticado(UUID id, String rol) {

	public boolean esAdmin() {
		return "ADMIN".equals(rol);
	}
}
