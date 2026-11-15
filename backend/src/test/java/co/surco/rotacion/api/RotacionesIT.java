package co.surco.rotacion.api;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.surco.recomendacion.PlanesDePrueba;
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
 * Rotaciones y selección contra PostgreSQL (migraciones V5 y V6), de punta a punta en el backend: fincas
 * -> recomendacion -> rotacion, con el planificador doblado por una respuesta real. Necesita la base
 * surco_test; se ejecuta con ./mvnw verify.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RotacionesIT {

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
	void baseLimpia() {
		bd.execute("truncate identidad.usuarios cascade");
		given(motor.planificar(anyDouble(), anyDouble(), anyBoolean(), any())).willReturn(PlanesDePrueba.neiva());
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
	void calculaEligeYRecuerdaElCiclo() throws Exception {
		String carlos = tokenDe("3001234567");
		String lote = terrenoDe(carlos);
		String base = "/api/v1/lotes/" + lote;

		http.perform(conToken(get(base + "/rotaciones"), carlos)).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("ROTACION_SIN_CALCULAR"));

		String plan = http.perform(conToken(post(base + "/rotaciones"), carlos)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.rotaciones", hasSize(3)))
			.andExpect(jsonPath("$.rotaciones[0].grado").isString())
			.andExpect(jsonPath("$.rotaciones[0].agua_ahorrada").isString())
			.andExpect(jsonPath("$.ranking", hasSize(23)))
			.andExpect(jsonPath("$.lluvias.fuente").isString())
			.andExpect(jsonPath("$.seleccion").doesNotExist())
			.andReturn().getResponse().getContentAsString();
		String planId = JsonPath.read(plan, "$.id");
		String rotacionId = JsonPath.read(plan, "$.rotaciones[1].id");

		http.perform(conToken(put(base + "/seleccion"), carlos).contentType(MediaType.APPLICATION_JSON)
				.content("{\"plan_id\": \"%s\", \"rotacion_id\": \"%s\"}".formatted(planId, rotacionId)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.rotacion_id").value(rotacionId));
		http.perform(conToken(get(base + "/seleccion"), carlos)).andExpect(status().isOk())
			.andExpect(jsonPath("$.plan_id").value(planId));
		http.perform(conToken(get(base + "/rotaciones"), carlos)).andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(planId))
			.andExpect(jsonPath("$.seleccion.rotacion_id").value(rotacionId));
	}

	@Test
	void unLoteAjenoNoSeCalculaNiSeElige() throws Exception {
		String carlos = tokenDe("3001234567");
		String ana = tokenDe("3109876543");
		String lote = terrenoDe(carlos);
		String plan = http.perform(conToken(post("/api/v1/lotes/" + lote + "/rotaciones"), carlos))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

		http.perform(conToken(post("/api/v1/lotes/" + lote + "/rotaciones"), ana)).andExpect(status().isNotFound());
		http.perform(conToken(get("/api/v1/lotes/" + lote + "/rotaciones"), ana)).andExpect(status().isNotFound());
		http.perform(conToken(put("/api/v1/lotes/" + lote + "/seleccion"), ana).contentType(MediaType.APPLICATION_JSON)
				.content("{\"plan_id\": \"%s\", \"rotacion_id\": \"%s\"}".formatted(JsonPath.read(plan, "$.id"),
					JsonPath.read(plan, "$.rotaciones[0].id"))))
			.andExpect(status().isNotFound());
		http.perform(put("/api/v1/lotes/" + lote + "/seleccion")).andExpect(status().isUnauthorized());
	}
}
