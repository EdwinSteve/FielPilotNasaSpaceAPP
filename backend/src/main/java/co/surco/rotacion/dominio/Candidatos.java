package co.surco.rotacion.dominio;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que la rotación recibe de la recomendación del terreno (módulo recomendacion, por su API pública).
 *
 * @param primera  los cultivos evaluados con el pronóstico para la primera siembra (todos los que puntúa el
 *                 modelo, aptos y no aptos)
 * @param historico frecuencia de éxito en años pasados para las siembras siguientes
 */
public record Candidatos(UUID recomendacionId, String nombreTerreno, double areaHa, boolean riego, String motor,
	List<Cultivo> primera, Historico historico, List<CultivoRanking> ranking, Lluvias lluvias) {

	/**
	 * @param fijacionKgHa kg de N por hectárea que fija (leguminosas); null si no fija
	 * @param riegoMm      riego que haría falta en el año típico para la primera siembra
	 */
	public record Cultivo(String id, String nombre, String familia, String grupo, boolean fijaNitrogeno, Integer fijacionKgHa,
		int cicloDias, LocalDate siembra, LocalDate cosecha, double puntaje, double probExito, double riegoMm,
		boolean apto) {

		/** Un abono verde (crotalaria) mejora el suelo pero no se vende. */
		public boolean abonoVerde() {
			return "abono_verde".equals(grupo);
		}
	}

	/** Series alineadas con {@code fechas} (siembras cada 15 días); null donde no hubo años con datos. */
	public record Historico(String fuente, List<LocalDate> fechas, Map<String, List<Double>> probExito,
		Map<String, List<Double>> riegoMm) {
	}

	/** Una fila del ranking que ve el agricultor (espejo de CultivoRanking del frontend). */
	public record CultivoRanking(String id, String nombre, double puntaje, boolean apto, String razonNoApto) {
	}

	public record Lluvias(String fuente, List<LluviaMes> meses) {
	}

	public record LluviaMes(String mes, int lluviaMm, String nivel, String nota) {
	}
}
