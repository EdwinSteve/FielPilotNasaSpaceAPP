package co.surco.alertas.dominio;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import java.util.SortedMap;

/**
 * Compara la lluvia de los últimos 30 días con la lluvia normal de esos mismos días.
 *
 * @param porcentaje 100 = llovió lo normal; 50 = la mitad
 * @param hasta      último día con dato
 * @param epocaSeca  lo normal es tan poco que es época seca habitual: se trata como NORMAL
 */
public record IndicadorSequia(double lluviaMm, double normalMm, double porcentaje, LocalDate hasta, boolean epocaSeca) {

	public static final int DIAS_VENTANA = 30;
	private static final int DIAS_MINIMOS = 25;
	/** Con menos de esto de lluvia normal, cualquier diferencia dispara el porcentaje y daría falsas alarmas. */
	private static final double NORMAL_MINIMO_MM = 20.0;

	public NivelSequia nivel() {
		return NivelSequia.desdePorcentaje(porcentaje);
	}

	/**
	 * @param lluviaDiaria mm por día, solo días con dato válido
	 * @param normalMmDia  lluvia normal en mm/día de cada mes
	 * @throws co.surco.compartido.error.ErrorDominio FUENTE_NO_DISPONIBLE si no hay datos suficientes
	 */
	public static IndicadorSequia calcular(SortedMap<LocalDate, Double> lluviaDiaria, Map<Month, Double> normalMmDia) {
		if (lluviaDiaria.isEmpty()) {
			throw ErroresAlertas.fuenteNoDisponible("la lluvia de la NASA");
		}
		LocalDate hasta = lluviaDiaria.lastKey();
		LocalDate desde = hasta.minusDays(DIAS_VENTANA - 1L);

		double lluvia = 0;
		double normal = 0;
		int dias = 0;
		for (Map.Entry<LocalDate, Double> dia : lluviaDiaria.tailMap(desde).entrySet()) {
			Double normalDelMes = normalMmDia.get(dia.getKey().getMonth());
			if (normalDelMes == null) {
				throw ErroresAlertas.fuenteNoDisponible("la lluvia normal de la NASA");
			}
			lluvia += dia.getValue();
			normal += normalDelMes; // solo se suman los días que sí tienen dato
			dias++;
		}
		if (dias < DIAS_MINIMOS) {
			throw ErroresAlertas.fuenteNoDisponible("la lluvia de la NASA");
		}

		boolean epocaSeca = normal < NORMAL_MINIMO_MM;
		double porcentaje = epocaSeca ? 100.0 : lluvia / normal * 100.0;
		return new IndicadorSequia(lluvia, normal, porcentaje, hasta, epocaSeca);
	}
}
