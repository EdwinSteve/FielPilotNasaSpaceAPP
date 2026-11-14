package co.surco.compartido.seguridad;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Seguridad de toda la API.
 *
 * <ul>
 *   <li>Sin estado: no hay sesión de servidor ni cookies; cada petición trae {@code Authorization: Bearer <jwt>}.</li>
 *   <li>Spring Security valida el JWT (firma HS256, vencimiento, emisor y audiencia) antes de llegar al controlador.</li>
 *   <li>Seguro por defecto: toda ruta exige token salvo las de {@link RutasPublicas}.</li>
 *   <li>{@code @EnableMethodSecurity}: los controladores pueden pedir roles con {@code @PreAuthorize}.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class ConfiguracionSeguridad {

	@Bean
	SecurityFilterChain cadenaSeguridad(HttpSecurity http, JwtDecoder decodificador) throws Exception {
		http
			// API con tokens Bearer en encabezado: no hay cookies de sesión que proteger contra CSRF
			.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> {})
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(rutas -> {
				for (RutasPublicas.Ruta r : RutasPublicas.RUTAS) {
					rutas.requestMatchers(r.metodo(), r.patron()).permitAll();
				}
				// Los errores internos de Spring (/error) no deben convertir un 404 en 401
				rutas.requestMatchers("/error").permitAll();
				rutas.anyRequest().authenticated();
			})
			.oauth2ResourceServer(rs -> rs
				.jwt(jwt -> jwt.decoder(decodificador).jwtAuthenticationConverter(new ConvertidorJwt()))
				.authenticationEntryPoint(RespuestasSeguridad.noAutenticado())
				.accessDeniedHandler(RespuestasSeguridad.sinPermiso()))
			.exceptionHandling(e -> e
				.authenticationEntryPoint(RespuestasSeguridad.noAutenticado())
				.accessDeniedHandler(RespuestasSeguridad.sinPermiso()));
		return http.build();
	}

	/** 64 caracteres: holgura sobre los 256 bits que exige HS256. El mensaje de error nunca incluye el valor. */
	static final int LARGO_MINIMO_SECRETO = 64;

	@Bean
	SecretKey claveJwt(PropiedadesJwt propiedades) {
		String secreto = propiedades.secreto();
		if (secreto == null || secreto.isBlank()) {
			throw new IllegalStateException("Falta JWT_SECRETO. Genere uno con: openssl rand -base64 48");
		}
		if (secreto.length() < LARGO_MINIMO_SECRETO) {
			throw new IllegalStateException("JWT_SECRETO debe tener al menos " + LARGO_MINIMO_SECRETO
				+ " caracteres (tiene " + secreto.length() + "). Genere uno con: openssl rand -base64 48");
		}
		return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	JwtEncoder codificadorJwt(SecretKey clave) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
	}

	@Bean
	JwtDecoder decodificadorJwt(SecretKey clave, PropiedadesJwt propiedades) {
		return decodificador(clave, propiedades);
	}

	/** Decodificador con todas las validaciones. Público para probarlo sin levantar Spring. */
	public static JwtDecoder decodificador(SecretKey clave, PropiedadesJwt propiedades) {
		NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
		OAuth2TokenValidator<Jwt> audiencia = jwt -> jwt.getAudience() != null
			&& jwt.getAudience().contains(propiedades.audiencia())
			? OAuth2TokenValidatorResult.success()
			: OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audiencia no válida", null));
		// createDefaultWithIssuer valida vencimiento (exp/nbf, con 60 s de tolerancia) y emisor
		decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
			JwtValidators.createDefaultWithIssuer(propiedades.emisor()), audiencia));
		return decodificador;
	}

	@Bean
	CorsConfigurationSource cors(@Value("${surco.cors.origenes}") List<String> origenes) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(origenes);
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/api/**", config);
		return fuente;
	}
}
