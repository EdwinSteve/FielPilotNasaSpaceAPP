package co.surco.recomendacion.dominio;

import java.time.LocalDate;
import java.util.List;

/**
 * Lo que responde el planificador estacional (ml/planificador, servidor.py) para un terreno: el clima
 * esperado de los próximos meses y el ranking de cultivos para cada fecha de siembra del próximo mes.
 * Los rendimientos son relativos al potencial del cultivo (1 = el máximo en buenas condiciones).
 */
public record PlanCiclo(Lote lote, LocalDate generado, Pronostico pronostico, Mejor mejor, List<Opcion> opciones,
	String modelo) {

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
	 * @param razonNoApto     por qué no se recomienda (por ejemplo, la altitud); null si es apto
	 */
	public record Cultivo(String cultivo, String nombre, int cicloDias, LocalDate cosecha, double rendP10,
		double rendP50, double rendP90, double probExito, double riegoMmP50, double rendConRiegoP50, boolean apto,
		String razonNoApto) {
	}
}
