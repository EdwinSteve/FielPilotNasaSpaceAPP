package co.surco.recomendacion.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.surco.recomendacion.PlanesDePrueba;
import co.surco.recomendacion.dominio.ErroresRecomendacion;
import co.surco.recomendacion.dominio.Puertos.MotorPlanificador;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Las recomendaciones por lote contra PostgreSQL (migración V5), con el planificador doblado por una
 * respuesta real (plan-neiva-bf.json). Necesita la base surco_test; se ejecuta con ./mvnw verify.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecomendacionesIT {

	static final String TERRENO = """
		{"nombre": "El Mirador", "municipio": "41001", "riego": false,
		 "vertices": [{"lat": 2.927, "lon": -75.288}, {"lat": 2.927, "lon": -75.287},
		              {"lat": 2.928, "lon": -75.287}, {"lat": 2.928, "lon": -75.288}]}""";

	@Autowired
	MockMvc http;
	@Autowired
	JdbcTemplate bd;
	@MockitoBean
	MotorPlanificador motor;

	@BeforeEach
	void baseLimpia() throws Exception {
		bd.execute("truncate identidad.usuarios cascade"); // borra también terrenos y recomendaciones
		willReturn(PlanesDePrueba.neiva()).given(motor).planificar(anyDouble(), anyDouble(), anyBoolean(), any());
	}

	String tokenDe(String celular) throws Exception {
		String registro = """
			{"nombre_completo": "Carlos Perdomo", "celular": "%s", "contrasena": "MiFinca2026",
			 "municipio_residencia": "41001", "acepta_politica": true, "version_politica": "1.1"}""".formatted(celular);
		String json = http.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(json, "$.tokens.access_token");
	}

	String terrenoDe(String token) throws Exception {
		String json = http.perform(conToken(post("/api/v1/fincas/terrenos"), token)
				.contentType(MediaType.APPLICATION_JSON).content(TERRENO))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(json, "$.id");
	}

	static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder r, String token) {
		return r.header("Authorization", "Bearer " + token);
	}

	@Test
	void calculaGuardaYLeeLaUltima() throws Exception {
		String carlos = tokenDe("3001234567");
		String lote = terrenoDe(carlos);
		String base = "/api/v1/lotes/" + lote + "/recomendaciones";

		http.perform(conToken(get(base + "/actual"), carlos)).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("RECOMENDACION_NO_ENCONTRADA"));

		String creada = http.perform(conToken(post(base), carlos)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.terreno_id").value(lote))
			.andExpect(jsonPath("$.motor").value("bf"))
			.andExpect(jsonPath("$.ranking", hasSize(23)))
			.andExpect(jsonPath("$.ranking[0].apto").value(true))
			.andExpect(jsonPath("$.ranking[0].puntaje").isNumber())
			.andExpect(jsonPath("$.ranking[*].id", not(hasItem("cafe"))))
			.andExpect(jsonPath("$.lluvias.meses").isNotEmpty())
			.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(creada, "$.id");

		http.perform(conToken(get(base + "/actual"), carlos)).andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(id));
		http.perform(conToken(get(base + "/" + id), carlos)).andExpect(status().isOk())
			.andExpect(jsonPath("$.ranking", hasSize(23)));
	}

	@Test
	void unLoteAjenoEs404YSinTokenEs401() throws Exception {
		String carlos = tokenDe("3001234567");
		String ana = tokenDe("3109876543");
		String lote = terrenoDe(carlos);
		http.perform(conToken(post("/api/v1/lotes/" + lote + "/recomendaciones"), ana)).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("RECOMENDACION_TERRENO_NO_ENCONTRADO"));
		http.perform(post("/api/v1/lotes/" + lote + "/recomendaciones")).andExpect(status().isUnauthorized());
	}

	@Test
	void losErroresDelModeloLleganConSuEstado() throws Exception {
		String carlos = tokenDe("3001234567");
		String base = "/api/v1/lotes/" + terrenoDe(carlos) + "/recomendaciones";
		// willThrow(...).given(mock): given(mock.planificar(...)) llamaría al mock ya configurado y lanzaría
		willThrow(ErroresRecomendacion.fuenteCaida("NASA POWER")).given(motor)
			.planificar(anyDouble(), anyDouble(), anyBoolean(), any());
		http.perform(conToken(post(base), carlos)).andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.code").value("RECOMENDACION_FUENTE_CAIDA"))
			.andExpect(jsonPath("$.details[0].field").value("fuente"))
			.andExpect(jsonPath("$.details[0].message").value("NASA POWER"));
		willThrow(ErroresRecomendacion.terrenoSinSuelo()).given(motor)
			.planificar(anyDouble(), anyDouble(), anyBoolean(), any());
		http.perform(conToken(post(base), carlos)).andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("RECOMENDACION_TERRENO_SIN_SUELO"));
		http.perform(conToken(post(base).param("siembra", "2020-01-01"), carlos)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("RECOMENDACION_SIEMBRA_INVALIDA"));
	}
}
