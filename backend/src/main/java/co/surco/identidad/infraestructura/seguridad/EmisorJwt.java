package co.surco.identidad.infraestructura.seguridad;

import co.surco.compartido.seguridad.ConvertidorJwt;
import co.surco.compartido.seguridad.PropiedadesJwt;
import co.surco.identidad.dominio.Puertos.EmisorTokensAcceso;
import co.surco.identidad.dominio.Usuario;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Emite el access token: JWT firmado con HS256.
 *
 * <p>Claims: {@code sub} (id del usuario), {@code rol}, {@code iss}, {@code aud}, {@code iat}, {@code exp}
 * y {@code jti}. Nunca lleva celular, nombre ni otros datos personales: un JWT se puede leer
 * (Base64) aunque no se pueda falsificar.
 */
@Component
class EmisorJwt implements EmisorTokensAcceso {

	private final JwtEncoder codificador;
	private final PropiedadesJwt propiedades;

	EmisorJwt(JwtEncoder codificador, PropiedadesJwt propiedades) {
		this.codificador = codificador;
		this.propiedades = propiedades;
	}

	@Override
	public String emitir(Usuario usuario, Instant ahora) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(propiedades.emisor())
			.audience(List.of(propiedades.audiencia()))
			.subject(usuario.id().toString())
			.issuedAt(ahora)
			.expiresAt(ahora.plus(propiedades.duracionAcceso()))
			.id(UUID.randomUUID().toString())
			.claim(ConvertidorJwt.CLAIM_ROL, usuario.rol().name())
			.build();
		JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
		return codificador.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
	}

	@Override
	public long duracionSegundos() {
		return propiedades.duracionAcceso().toSeconds();
	}
}
