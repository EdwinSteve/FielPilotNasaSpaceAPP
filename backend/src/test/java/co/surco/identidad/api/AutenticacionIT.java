package co.surco.identidad.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.surco.compartido.seguridad.PropiedadesJwt;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * La API completa contra PostgreSQL: migraciones, filtro JWT, casos de uso y errores.
 * Necesita la base surco_test (ver backend/compose.yaml). Se ejecuta con ./mvnw verify.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AutenticacionIT {

	static final String REGISTRO = """
		{"nombre_completo": "Ana Perdomo", "celular": "300 123 4567", "correo": "ana@correo.co",
		 "contrasena": "MiFinca2026", "municipio_residencia": "52356", "acepta_politica": true,
		 "version_politica": "1.0"}""";

	@Autowired
	MockMvc http;
	@Autowired
	JdbcTemplate bd;
	@Autowired
	JwtEncoder codificador;
	@Autowired
	PropiedadesJwt jwt;

	@BeforeEach
	void baseLimpia() {
		bd.execute("truncate identidad.usuarios cascade");
	}

	ResultActions postJson(String ruta, String cuerpo) throws Exception {
		return http.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
	}

	String registrar() throws Exception {
		return postJson("/api/v1/auth/registro", REGISTRO).andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
	}

	String login(String contrasena) throws Exception {
		return postJson("/api/v1/auth/login", "{\"celular\":\"3001234567\",\"contrasena\":\"" + contrasena + "\"}")
			.andReturn().getResponse().getContentAsString();
	}

	static String campo(String json, String ruta) {
		return JsonPath.read(json, ruta);
	}

	@Nested
	class RutasPublicas {

		@Test
		void saludPoliticaYTerritorioNoPidenToken() throws Exception {
			http.perform(get("/api/v1/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
			http.perform(get("/api/v1/politicas/vigente")).andExpect(status().isOk())
				.andExpect(jsonPath("$.version").value("1.0"))
				.andExpect(jsonPath("$.url_texto").value("/politica-de-datos"));
			http.perform(get("/api/v1/territorio/departamentos")).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(33)));
			http.perform(get("/api/v1/territorio/municipios").param("departamento", "05"))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(125)));
			http.perform(get("/api/v1/territorio/municipios").param("departamento", "00"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("DEPARTAMENTO_NO_ENCONTRADO"));
			http.perform(get("/api/v1/territorio/municipios"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PARAMETRO_REQUERIDO"));
		}
	}

	@Nested
	class RutasProtegidas {

		@Test
		void sinTokenResponde401EnJson() throws Exception {
			http.perform(get("/api/v1/usuarios/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", "Bearer"))
				.andExpect(jsonPath("$.code").value("AUTH_NO_AUTENTICADO"));
			// Seguro por defecto: una ruta cualquiera sin declarar también exige token
			http.perform(get("/api/v1/fincas")).andExpect(status().isUnauthorized());
		}

		@Test
		void conTokenFalsoOVencidoResponde401TokenInvalido() throws Exception {
			http.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer no.es.un-jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", startsWith("Bearer error=\"invalid_token\"")))
				.andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALIDO"));

			String id = campo(registrar(), "$.usuario.id");
			Instant hace = Instant.now().minus(Duration.ofHours(1));
			JwtClaimsSet vencido = JwtClaimsSet.builder().issuer(jwt.emisor()).audience(List.of(jwt.audiencia()))
				.subject(id).claim("rol", "PRODUCTOR").issuedAt(hace).expiresAt(hace.plusSeconds(900)).build();
			String token = codificador.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
				vencido)).getTokenValue();
			http.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALIDO"));
		}

		@Test
		void elControladorRecibeElUsuarioDelToken() throws Exception {
			String registro = registrar();
			String access = campo(registro, "$.tokens.access_token");
			http.perform(get("/api/v1/prueba/protegida").header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.usuario").value(campo(registro, "$.usuario.id")))
				.andExpect(jsonPath("$.rol").value("PRODUCTOR"));
		}

		@Test
		void preAuthorizeExigeElRol() throws Exception {
			String access = campo(registrar(), "$.tokens.access_token");
			http.perform(get("/api/v1/prueba/admin").header("Authorization", "Bearer " + access))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("AUTH_SIN_PERMISO"));

			bd.update("update identidad.usuarios set rol = 'ADMIN' where celular = '3001234567'");
			String accessAdmin = campo(login("MiFinca2026"), "$.access_token");
			http.perform(get("/api/v1/prueba/admin").header("Authorization", "Bearer " + accessAdmin))
				.andExpect(status().isOk());
		}
	}

	@Nested
	class Sesion {

		@Test
		void registroLoginPerfilRefreshYLogout() throws Exception {
			// Registro: 201 con usuario y tokens, en snake_case
			String registro = registrar();
			assertThat(campo(registro, "$.usuario.celular")).isEqualTo("3001234567");
			assertThat(campo(registro, "$.usuario.departamento_residencia")).isEqualTo("52");
			assertThat(campo(registro, "$.tokens.token_type")).isEqualTo("bearer");
			assertThat((Integer) JsonPath.read(registro, "$.tokens.expira_en")).isEqualTo(900);

			// En la base: contraseña con BCrypt, refresh token solo como hash, consentimiento guardado
			assertThat(bd.queryForObject("select contrasena_hash from identidad.usuarios", String.class))
				.startsWith("{bcrypt}");
			String refreshRegistro = campo(registro, "$.tokens.refresh_token");
			assertThat(bd.queryForObject("select count(*) from identidad.sesiones where hash_token = ?", Integer.class,
				refreshRegistro)).isZero();
			assertThat(bd.queryForObject("select version_politica from identidad.consentimientos", String.class))
				.isEqualTo("1.0");

			// Login y perfil con el access token
			String tokens = login("MiFinca2026");
			http.perform(get("/api/v1/usuarios/me").header("Authorization", "Bearer " + campo(tokens, "$.access_token")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre_completo").value("Ana Perdomo"))
				.andExpect(jsonPath("$.municipio_residencia").value("52356"))
				.andExpect(jsonPath("$.rol").value("PRODUCTOR"));

			// Refresh: rota el token
			String refresh = campo(tokens, "$.refresh_token");
			String nuevos = postJson("/api/v1/auth/refresh", "{\"refresh_token\":\"" + refresh + "\"}")
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
			String refreshNuevo = campo(nuevos, "$.refresh_token");
			assertThat(refreshNuevo).isNotEqualTo(refresh);

			// Reutilizar el viejo (robo): 401 y se revoca también el nuevo
			postJson("/api/v1/auth/refresh", "{\"refresh_token\":\"" + refresh + "\"}")
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTH_REFRESH_INVALIDO"));
			postJson("/api/v1/auth/refresh", "{\"refresh_token\":\"" + refreshNuevo + "\"}")
				.andExpect(status().isUnauthorized());

			// Logout de la sesión del registro: 204 y su refresh token deja de servir
			postJson("/api/v1/auth/logout", "{\"refresh_token\":\"" + refreshRegistro + "\"}")
				.andExpect(status().isNoContent());
			postJson("/api/v1/auth/refresh", "{\"refresh_token\":\"" + refreshRegistro + "\"}")
				.andExpect(status().isUnauthorized());
		}

		@Test
		void logoutTodoExigeTokenYCierraTodasLasSesiones() throws Exception {
			registrar();
			String a = login("MiFinca2026");
			String b = login("MiFinca2026");
			http.perform(post("/api/v1/auth/logout-todo")).andExpect(status().isUnauthorized());
			http.perform(post("/api/v1/auth/logout-todo").header("Authorization", "Bearer " + campo(a, "$.access_token")))
				.andExpect(status().isNoContent());
			for (String sesion : List.of(a, b)) {
				postJson("/api/v1/auth/refresh", "{\"refresh_token\":\"" + campo(sesion, "$.refresh_token") + "\"}")
					.andExpect(status().isUnauthorized());
			}
		}

		@Test
		void bloqueaLaCuentaTrasCincoIntentosFallidos() throws Exception {
			registrar();
			for (int i = 0; i < 4; i++) {
				assertThat(campo(login("Equivocada1"), "$.code")).isEqualTo("AUTH_CREDENCIALES_INVALIDAS");
			}
			postJson("/api/v1/auth/login", "{\"celular\":\"3001234567\",\"contrasena\":\"Equivocada1\"}")
				.andExpect(status().isLocked()).andExpect(jsonPath("$.code").value("AUTH_CUENTA_BLOQUEADA"));
			// El bloqueo quedó guardado aunque la respuesta fue un error
			assertThat(bd.queryForObject("select bloqueado_hasta is not null from identidad.usuarios", Boolean.class))
				.isTrue();
			postJson("/api/v1/auth/login", "{\"celular\":\"3001234567\",\"contrasena\":\"MiFinca2026\"}")
				.andExpect(status().isLocked());
		}
	}

	@Nested
	class ErroresDeRegistro {

		@Test
		void camposFaltantesDan422ConElNombreDelCampoEnElJson() throws Exception {
			postJson("/api/v1/auth/registro", "{\"celular\":\"3001234567\"}")
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("VALIDATION"))
				.andExpect(jsonPath("$.details[?(@.field == 'nombre_completo')]").exists())
				.andExpect(jsonPath("$.details[?(@.field == 'municipio_residencia')]").exists());
		}

		@Test
		void reglasDeNegocioDanErrorPorCampo() throws Exception {
			postJson("/api/v1/auth/registro", REGISTRO.replace("300 123 4567", "12345"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details[0].field").value("celular"));
			postJson("/api/v1/auth/registro", REGISTRO.replace("52356", "99999"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details[0].field").value("municipio_residencia"));
			postJson("/api/v1/auth/registro", REGISTRO.replace("\"version_politica\": \"1.0\"", "\"version_politica\": \"0.1\""))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("POLITICA_DESACTUALIZADA"));
		}

		@Test
		void celularDuplicadoDa409() throws Exception {
			registrar();
			postJson("/api/v1/auth/registro", REGISTRO.replace("ana@correo.co", "otra@correo.co"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("USUARIO_CELULAR_DUPLICADO"));
		}

		@Test
		void jsonMalFormadoDa400() throws Exception {
			postJson("/api/v1/auth/login", "{no es json").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("JSON_INVALIDO"));
		}
	}
}
