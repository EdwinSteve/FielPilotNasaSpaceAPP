package co.surco.rotacion.dominio;

import java.time.LocalDate;
import java.util.List;

/**
 * Una secuencia de 2-3 cultivos para el mismo lote, uno detrás de otro.
 *
 * @param id               los ids de los cultivos unidos con "-" ("maiz-frijol-sorgo"): estable entre cálculos
 * @param puntaje          la media de P(éxito) más el bono por leguminosa, de 0 a 1 (ordena y da el grado)
 * @param probMedia        la media de P(éxito) de sus temporadas, sin bono
 * @param frecuenciaExito  round(probMedia * 10): "le va bien N de cada 10 años"
 * @param aguaAhorradaM3   riego evitado frente a repetir el primer cultivo en las mismas fechas (m³, >= 0)
 * @param aporteNitrogeno  qué leguminosa deja nitrógeno a cuál cultivo; null si no hay leguminosa útil
 */
public record Rotacion(String id, String grado, List<Temporada> temporadas, double puntaje, double probMedia,
	int frecuenciaExito, long aguaAhorradaM3, String aporteNitrogeno) {

	/**
	 * @param fuente "pronostico" (primera temporada, con el pronóstico de los próximos meses) o "historico"
	 *               (temporadas siguientes, con la frecuencia de éxito en años pasados)
	 */
	public record Temporada(String cultivo, String nombre, String familia, LocalDate siembra, LocalDate cosecha,
		double probExito, double riegoMm, String fuente) {
	}

	public List<String> secuencia() {
		return temporadas.stream().map(Temporada::nombre).toList();
	}
}
