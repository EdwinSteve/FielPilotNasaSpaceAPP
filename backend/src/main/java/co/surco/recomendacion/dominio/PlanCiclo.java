package co.surco.recomendacion.dominio;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Lo que responde el planificador estacional (ml/planificador, servidor.py) para un terreno: el clima
 * esperado de los próximos meses y el ranking de cultivos para cada fecha de siembra del próximo mes.
 * Los rendimientos son relativos al potencial del cultivo (1 = el máximo en buenas condiciones).
 */
public record PlanCiclo(Lote lote, LocalDate generado, Pronostico pronostico, Mejor mejor, List<Opcion> opciones,
	Historico historico, String modelo, Motor motor) {

	/** Lo que el modelo leyó del terreno (elevación de Open-Meteo, pH de SoilGrids). */
	public record Lote(double lat, double lon, double elevacionM, double phSuelo, boolean riego) {
	}

	/**
	 * @param fuente      "analogos" (años parecidos de NASA POWER según El Niño/La Niña) o "c3s" (SEAS5)
	 * @param faseEnso    "nino", "nina" o "neutral"
	 * @param panorama    bloques de 30 días desde la primera fecha de siembra
	 */
	public record Pronostico(String fuente, double oni, double oniTendencia, String faseEnso, List<Bloque> panorama) {
	}

	/** Clima esperado de 30 días: lluvia del escenario seco (p10), típico (p50) y húmedo (p90) frente a lo normal. */
	public record Bloque(LocalDate desde, LocalDate hasta, int lluviaMmP10, int lluviaMmP50, int lluviaMmP90,
		int lluviaNormalMm, int anomaliaLluviaPct, double tmaxC, double tminC, double tmaxNormalC) {
	}

	/** El mejor ciclo: el cultivo apto con mayor rendimiento esperado y cuándo sembrarlo. */
	public record Mejor(LocalDate siembra, Cultivo cultivo) {
	}

	public record Opcion(LocalDate siembra, LocalDate inicioPronostico, List<Cultivo> cultivos) {
	}

	/**
	 * Un cultivo para una fecha de siembra.
	 *
	 * @param probExito       probabilidad de lograr al menos el 60 % del rendimiento potencial
	 * @param riegoMmP50      riego que haría falta en el escenario típico
	 * @param razonNoApto     por qué no se recomienda (altitud o pH del suelo); null si es apto
	 * @param familia         familia botánica (catálogo), para no repetirla en la rotación
	 * @param fijacionNitrogenoKgHa   kg de N por hectárea que fija si es leguminosa; null si no fija
	 */
	public record Cultivo(String cultivo, String nombre, int cicloDias, LocalDate cosecha, double rendP10,
		double rendP50, double rendP90, double probExito, double riegoMmP50, double rendConRiegoP50, boolean apto,
		String razonNoApto, String familia, Boolean fijaNitrogeno, Integer fijacionNitrogenoKgHa) {

		/** Un planificador anterior no manda {@code fija_nitrogeno}: se toma como que no fija. */
		public boolean fija() {
			return Boolean.TRUE.equals(fijaNitrogeno);
		}
	}

	/**
	 * Para las temporadas siguientes de una rotación (fuera de los 175 días del pronóstico): con qué
	 * frecuencia cada cultivo habría salido bien en los años pasados del terreno, sembrando en cada fecha.
	 *
	 * @param fechas    siembras cada 15 días, de 30 a 540 días después de generar el plan
	 * @param cultivos  por id de cultivo, series alineadas con {@code fechas} (null: sin años con datos)
	 */
	public record Historico(String fuente, List<LocalDate> fechas, Map<String, Serie> cultivos) {
	}

	public record Serie(List<Double> probExito, List<Double> rendP50, List<Double> riegoMmP50,
		List<Double> rendConRiegoP50) {
	}

	/** Con qué se puntuó: "bf" (oráculo sobre el pronóstico, el que se sirve) o "red". */
	public record Motor(String id, String descripcion) {
	}
}
