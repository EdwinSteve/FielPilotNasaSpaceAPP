package co.surco.fincas.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * La API de terrenos contra PostgreSQL: migración V4, relación con el usuario y aislamiento entre
 * productores. Necesita la base surco_test (ver backend/compose.yaml). Se ejecuta con ./mvnw verify.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TerrenosIT {

	static final String TERRENO = """
		{"nombre": "Lote de arriba", "municipio": "17001", "riego": true,
		 "vertices": [{"lat": 5.06, "lon": -75.52}, {"lat": 5.06, "lon": -75.518},
		              {"lat": 5.062, "lon": -75.518}, {"lat": 5.062, "lon": -75.52}]}""";

	@Autowired
	MockMvc http;
	@Autowired
	JdbcTemplate bd;

	@BeforeEach
	void baseLimpia() {
		bd.execute("truncate identidad.usuarios cascade"); // la llave foránea borra también sus terrenos
	}

	String tokenDe(String celular) throws Exception {
		String registro = """
			{"nombre_completo": "Ana Perdomo", "celular": "%s", "contrasena": "MiFinca2026",
			 "municipio_residencia": "17001", "acepta_politica": true, "version_politica": "1.1"}""".formatted(celular);
		String json = http.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(registro))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(json, "$.tokens.access_token");
	}

	static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder r, String token) {
		return r.header("Authorization", "Bearer " + token);
	}

	@Test
	void registraListaYBorraSoloLosPropios() throws Exception {
		String ana = tokenDe("3001234567");
		String beto = tokenDe("3109876543");

		String creado = http.perform(conToken(post("/api/v1/fincas/terrenos"), ana)
				.contentType(MediaType.APPLICATION_JSON).content(TERRENO))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.nombre").value("Lote de arriba"))
			.andExpect(jsonPath("$.departamento").value("17"))
			.andExpect(jsonPath("$.vertices", hasSize(4)))
			.andExpect(jsonPath("$.area_ha").value(closeTo(4.9, 0.1)))
			.andExpect(jsonPath("$.centroide.lat").value(closeTo(5.061, 1e-6)))
			.andExpect(jsonPath("$.poligono_geojson").value(startsWith("{\"type\":\"Polygon\"")))
			.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(creado, "$.id");

		http.perform(conToken(get("/api/v1/fincas/terrenos"), ana)).andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(id));
		http.perform(conToken(get("/api/v1/fincas/terrenos"), beto)).andExpect(jsonPath("$", hasSize(0)));
		http.perform(conToken(get("/api/v1/fincas/terrenos/" + id), beto)).andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("FINCAS_TERRENO_NO_ENCONTRADO"));
		http.perform(conToken(delete("/api/v1/fincas/terrenos/" + id), beto)).andExpect(status().isNotFound());

		http.perform(conToken(delete("/api/v1/fincas/terrenos/" + id), ana)).andExpect(status().isNoContent());
		http.perform(conToken(get("/api/v1/fincas/terrenos"), ana)).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void sinTokenResponde401() throws Exception {
		http.perform(get("/api/v1/fincas/terrenos")).andExpect(status().isUnauthorized());
	}

	@Test
	void contornoInvalidoRespondeErrorDeCampo() throws Exception {
		String ana = tokenDe("3001234567");
		String cruzado = """
			{"nombre": "Lote", "municipio": "17001", "riego": false,
			 "vertices": [{"lat": 5.0, "lon": -75.5}, {"lat": 5.002, "lon": -75.498},
			              {"lat": 5.0, "lon": -75.498}, {"lat": 5.002, "lon": -75.5}]}""";
		http.perform(conToken(post("/api/v1/fincas/terrenos"), ana).contentType(MediaType.APPLICATION_JSON).content(cruzado))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FINCAS_POLIGONO_CRUZADO"))
			.andExpect(jsonPath("$.details[0].field").value("vertices"));
	}

	@Test
	void sugiereElMunicipioDeUnPunto() throws Exception {
		http.perform(get("/api/v1/territorio/municipios/cercano").param("lat", "5.068").param("lon", "-75.517"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.codigo").value("17001")); // Manizales
		http.perform(get("/api/v1/territorio/municipios/cercano").param("lat", "40.4").param("lon", "-3.7"))
			.andExpect(status().isNotFound());
	}
}
