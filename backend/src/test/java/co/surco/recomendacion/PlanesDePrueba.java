package co.surco.recomendacion;

import co.surco.recomendacion.dominio.PlanCiclo;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/** Respuestas reales del planificador (src/test/resources/planificador) para las pruebas de varios módulos. */
public final class PlanesDePrueba {

	/** Igual que la API: spring.jackson.property-naming-strategy = SNAKE_CASE. */
	public static final JsonMapper JSON = JsonMapper.builder()
		.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();

	private PlanesDePrueba() {
	}

	/** Neiva (446 m), motor Bf, generado el 2026-11-15: 23 cultivos, historico de 35 fechas. */
	public static PlanCiclo neiva() {
		return leer("/planificador/plan-neiva-bf.json");
	}

	/** Manizales (2116 m), formato de la primera versión del servidor (sin historico ni familias). */
	public static PlanCiclo manizales() {
		return leer("/planificador/plan-manizales.json");
	}

	private static PlanCiclo leer(String ruta) {
		try (InputStream in = PlanesDePrueba.class.getResourceAsStream(ruta)) {
			return JSON.readValue(in, PlanCiclo.class);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
