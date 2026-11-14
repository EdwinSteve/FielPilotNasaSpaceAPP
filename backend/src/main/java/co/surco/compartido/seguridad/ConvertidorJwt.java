package co.surco.compartido.seguridad;

import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

/** Convierte un JWT ya validado (firma, vencimiento, emisor, audiencia) en un {@link UsuarioAutenticado}. */
public class ConvertidorJwt implements Converter<Jwt, AbstractAuthenticationToken> {

	public static final String CLAIM_ROL = "rol";

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		String rol = jwt.getClaimAsString(CLAIM_ROL);
		if (jwt.getSubject() == null || rol == null) {
			throw new InvalidBearerTokenException("El token no trae usuario o rol");
		}
		try {
			return new AutenticacionUsuario(new UsuarioAutenticado(UUID.fromString(jwt.getSubject()), rol), jwt);
		} catch (IllegalArgumentException e) {
			throw new InvalidBearerTokenException("El sujeto del token no es un id válido");
		}
	}
}
