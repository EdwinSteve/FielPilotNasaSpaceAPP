package co.surco.identidad.infraestructura.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.surco.compartido.seguridad.ConfiguracionSeguridad;
import co.surco.compartido.seguridad.PropiedadesJwt;
import co.surco.identidad.dominio.Celular;
import co.surco.identidad.dominio.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** El JWT que emitimos es el que aceptamos; cualquier alteración se rechaza. Sin Spring ni base de datos. */
class JwtTest {

	static final PropiedadesJwt PROPIEDADES = new PropiedadesJwt("a".repeat(64), "surco-api", "surco-app",
		Duration.ofMinutes(15), Duration.ofDays(30));

	static SecretKey clave(String secreto) {
		return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	final SecretKey clave = clave(PROPIEDADES.secreto());
	final NimbusJwtEncoder codificador = new NimbusJwtEncoder(new ImmutableSecret<>(clave));
	final JwtDecoder decodificador = ConfiguracionSeguridad.decodificador(clave, PROPIEDADES);
	final Usuario usuario = Usuario.nuevo("Ana Perdomo", Celular.de("3001234567"), null, "h", "41001", Instant.now());

	@Test
	void emiteUnTokenConLosClaimsEsperadosYSinDatosPersonales() {
		String token = new EmisorJwt(codificador, PROPIEDADES).emitir(usuario, Instant.now());
		Jwt jwt = decodificador.decode(token);

		assertThat(jwt.getSubject()).isEqualTo(usuario.id().toString());
		assertThat(jwt.getClaimAsString("rol")).isEqualTo("PRODUCTOR");
		assertThat(jwt.getAudience()).containsExactly("surco-app");
		assertThat(jwt.getClaimAsString("iss")).isEqualTo("surco-api");
		assertThat(jwt.getId()).isNotBlank();
		assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
		assertThat(jwt.getClaims()).doesNotContainKeys("celular", "nombre", "nombre_completo", "correo");
		assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
	}

	@Test
	void rechazaUnTokenFirmadoConOtraClave() {
		var otro = new NimbusJwtEncoder(new ImmutableSecret<>(clave("b".repeat(64))));
		String falso = new EmisorJwt(otro, PROPIEDADES).emitir(usuario, Instant.now());
		assertThatThrownBy(() -> decodificador.decode(falso)).isInstanceOf(JwtException.class);
	}

	@Test
	void rechazaUnTokenAlterado() {
		String token = new EmisorJwt(codificador, PROPIEDADES).emitir(usuario, Instant.now());
		String[] partes = token.split("\\.");
		// Cambia el payload (por ejemplo, para subirse a ADMIN) sin poder recalcular la firma
		String payloadAdmin = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
			("{\"sub\":\"" + usuario.id() + "\",\"rol\":\"ADMIN\",\"iss\":\"surco-api\",\"aud\":[\"surco-app\"],"
				+ "\"exp\":" + (Instant.now().getEpochSecond() + 600) + "}").getBytes(StandardCharsets.UTF_8));
		assertThatThrownBy(() -> decodificador.decode(partes[0] + "." + payloadAdmin + "." + partes[2]))
			.isInstanceOf(JwtException.class);
	}

	@Test
	void rechazaUnTokenVencido() {
		String vencido = new EmisorJwt(codificador, PROPIEDADES)
			.emitir(usuario, Instant.now().minus(Duration.ofMinutes(20)));
		assertThatThrownBy(() -> decodificador.decode(vencido)).isInstanceOf(JwtException.class);
	}

	@Test
	void rechazaOtroEmisorUOtraAudiencia() {
		assertThatThrownBy(() -> decodificador.decode(firmar("otro-emisor", "surco-app"))).isInstanceOf(JwtException.class);
		assertThatThrownBy(() -> decodificador.decode(firmar("surco-api", "otra-app"))).isInstanceOf(JwtException.class);
		assertThat(decodificador.decode(firmar("surco-api", "surco-app")).getSubject()).isEqualTo("x");
	}

	@Test
	void rechazaTokensSinFirma() {
		String sinFirma = java.util.Base64.getUrlEncoder().withoutPadding()
			.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8)) + "."
			+ firmar("surco-api", "surco-app").split("\\.")[1] + ".";
		assertThatThrownBy(() -> decodificador.decode(sinFirma)).isInstanceOf(JwtException.class);
	}

	String firmar(String emisor, String audiencia) {
		Instant ahora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer(emisor).audience(List.of(audiencia)).subject("x")
			.issuedAt(ahora).expiresAt(ahora.plusSeconds(600)).claim("rol", "PRODUCTOR").build();
		return codificador.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}
}
