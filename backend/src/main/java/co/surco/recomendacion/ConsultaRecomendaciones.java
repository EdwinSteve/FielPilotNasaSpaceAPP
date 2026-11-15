package co.surco.recomendacion;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API pública del módulo recomendacion: la usa rotacion para armar secuencias de 2-3 temporadas.
 * Otros módulos solo usan esta interfaz y los records de este paquete, nunca las subcapas.
 */
public interface ConsultaRecomendaciones {

	/**
	 * Calcula (con el planificador estacional) y guarda la recomendación del terreno del productor.
	 * Lanza los errores de recomendación: terreno no encontrado (404), sin suelo (422), fuente caída (502)
	 * o modelo no disponible (503). La primera consulta de una zona puede tardar 1-3 minutos.
	 */
	RecomendacionTerreno calcular(UUID usuarioId, UUID terrenoId);

	/**
	 * Lo que el modelo recomienda para un terreno.
	 *
	 * @param cultivos  los cultivos que puntúa el modelo para la primera siembra (los aptos primero, por
	 *                  puntaje); solo los del modelo estacional, nunca perennes que no evalúa
	 * @param historico frecuencia de éxito de cada cultivo en años pasados, para las temporadas siguientes
	 */
	record RecomendacionTerreno(UUID id, UUID terrenoId, String nombreTerreno, double areaHa, boolean riego,
		Instant creadaEn, String motor, List<CultivoEvaluado> cultivos, HistoricoCultivos historico,
		LluviasEsperadas lluvias) {
	}

	/**
	 * Un cultivo evaluado para la primera siembra con el pronóstico de los próximos meses.
	 *
	 * @param puntaje       0-1, ver {@code co.surco.recomendacion.dominio.Puntaje}
	 * @param probExito     probabilidad de lograr al menos el 60 % del rendimiento potencial (en secano)
	 * @param fijacionNitrogenoKgHa nitrógeno que fija por hectárea si es leguminosa (catálogo); null si no fija
	 */
	record CultivoEvaluado(String id, String nombre, String familia, boolean fijaNitrogeno, Integer fijacionNitrogenoKgHa,
		int cicloDias, LocalDate siembra, LocalDate cosecha, double puntaje, double probExito, double riegoMmP50,
		boolean apto, String razonNoApto) {
	}

	/**
	 * Frecuencia de éxito y riego de cada cultivo en los años pasados del terreno, para siembras cada 15 días.
	 * Las listas van alineadas con {@code fechas}; un valor null significa que no hubo años con datos.
	 */
	record HistoricoCultivos(String fuente, List<LocalDate> fechas, Map<String, List<Double>> probExito,
		Map<String, List<Double>> riegoMmP50) {

		public static HistoricoCultivos vacio() {
			return new HistoricoCultivos("", List.of(), Map.of(), Map.of());
		}
	}

	/** Lluvia esperada por mes frente a lo normal, para la pantalla del plan. */
	record LluviasEsperadas(String fuente, List<LluviaMensual> meses) {
	}

	/**
	 * @param nivel "seco", "normal" o "lluvioso" (más de 20 % por debajo o por encima de lo normal)
	 * @param nota  qué hacer, en palabras sencillas; null si el mes es normal
	 */
	record LluviaMensual(String mes, int lluviaMm, String nivel, String nota) {
	}
}
