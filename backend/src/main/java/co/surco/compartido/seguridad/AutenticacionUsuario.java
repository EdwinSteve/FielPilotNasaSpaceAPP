package co.surco.compartido.seguridad;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Autenticación de Spring Security cuyo principal es un {@link UsuarioAutenticado}. */
public class AutenticacionUsuario extends AbstractAuthenticationToken {

	private final UsuarioAutenticado usuario;
	private final transient Jwt jwt;

	public AutenticacionUsuario(UsuarioAutenticado usuario, Jwt jwt) {
		// ROLE_PRODUCTOR o ROLE_ADMIN: habilita @PreAuthorize("hasRole('ADMIN')")
		super(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol())));
		this.usuario = usuario;
		this.jwt = jwt;
		setAuthenticated(true);
	}

	@Override
	public UsuarioAutenticado getPrincipal() {
		return usuario;
	}

	@Override
	public Jwt getCredentials() {
		return jwt;
	}

	@Override
	public String getName() {
		return usuario.id().toString();
	}
}
