package co.surco.recomendacion.infraestructura.planificador;

import static org.assertj.core.api.Assertions.assertThat;

import co.surco.compartido.error.ErrorDominio;
import co.surco.compartido.error.TipoError;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/** Cada error del planificador llega al productor con su código y su estado HTTP. */
class ClientePlanificadorTest {

	final ClientePlanificador cliente = new ClientePlanificador(RestClient.builder(), JsonMapper.builder().build(),
		"http://localhost:1", Duration.ofSeconds(1));

	ErrorDominio traducir(int estado, String cuerpo) {
		return cliente.traducir(estado, cuerpo, 2.9, -75.3);
	}

	@Test
	void terrenoSinSueloYMesNoEntrenadoSon422() {
		var e = traducir(422, "{\"code\": \"PLANIFICADOR_TERRENO_SIN_SUELO\", \"message\": \"SoilGrids...\"}");
		assertThat(e.codigo()).isEqualTo("RECOMENDACION_TERRENO_SIN_SUELO");
		assertThat(e.tipo()).isEqualTo(TipoError.NO_PROCESABLE);
		assertThat(traducir(422, "{\"code\": \"PLANIFICADOR_MES_NO_ENTRENADO\"}").codigo())
			.isEqualTo("RECOMENDACION_MES_NO_ENTRENADO");
	}

	@Test
	void fuenteCaidaEs502YDiceCual() {
		var e = traducir(502, "{\"code\": \"PLANIFICADOR_FUENTE_CAIDA\", \"message\": \"x\", \"fuente\": \"NASA POWER\"}");
		assertThat(e.codigo()).isEqualTo("RECOMENDACION_FUENTE_CAIDA");
		assertThat(e.tipo()).isEqualTo(TipoError.FUENTE_EXTERNA);
		assertThat(e.getMessage()).contains("NASA POWER");
		assertThat(e.detalles()).singleElement().satisfies(d -> {
			assertThat(d.field()).isEqualTo("fuente");
			assertThat(d.message()).isEqualTo("NASA POWER");
		});
	}

	@Test
	void otrosErroresDelClienteYDelServidor() {
		assertThat(traducir(400, "{\"code\": \"PLANIFICADOR_FUERA_DE_COLOMBIA\", \"message\": \"El terreno debe estar en Colombia.\"}")
			.getMessage()).contains("Colombia");
		assertThat(traducir(500, "{\"code\": \"PLANIFICADOR_ERROR\"}").tipo()).isEqualTo(TipoError.NO_DISPONIBLE);
		assertThat(traducir(502, "<html>Bad gateway</html>").tipo()).isEqualTo(TipoError.NO_DISPONIBLE);
	}

	@Test
	void sinConexionEs503() {
		var e = org.assertj.core.api.Assertions.catchThrowableOfType(ErrorDominio.class,
			() -> cliente.planificar(2.9, -75.3, false, null));
		assertThat(e.codigo()).isEqualTo("RECOMENDACION_NO_DISPONIBLE");
	}
}
