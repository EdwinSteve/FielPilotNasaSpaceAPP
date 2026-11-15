package co.surco.recomendacion.dominio;

import co.surco.recomendacion.ConsultaRecomendaciones.HistoricoCultivos;
import co.surco.recomendacion.ConsultaRecomendaciones.LluviaMensual;
import co.surco.recomendacion.ConsultaRecomendaciones.LluviasEsperadas;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Traducciones del plan del planificador a lo que se muestra: la lluvia esperada por mes y la tabla
 * histórica que usa la rotación.
 */
public final class Lluvias {

	/** Más de 20 % por debajo (o por encima) de lo normal: mes seco (o lluvioso). */
	static final int UMBRAL_PCT = 20;

	private static final Locale ES = Locale.of("es", "CO");

	private Lluvias() {
	}

	/**
	 * Un "mes" por cada bloque de 30 días del panorama (el primero empieza en la primera fecha de siembra),
	 * con el nombre del mes en que empieza. Como los bloques no van alineados con el calendario, dos pueden
	 * empezar en el mismo mes (1 y 31 de enero): entonces el segundo toma el mes siguiente, para que los
	 * nombres no se repitan. La lluvia es la del escenario típico (p50 de los miembros del pronóstico).
	 */
	public static LluviasEsperadas de(PlanCiclo plan) {
		PlanCiclo.Pronostico p = plan.pronostico();
		if (p == null || p.panorama() == null) {
			return new LluviasEsperadas(fuente(p), List.of());
		}
		List<LluviaMensual> meses = new ArrayList<>();
		Month anterior = null;
		for (PlanCiclo.Bloque b : p.panorama()) {
			Month m = b.desde().getMonth();
			if (m == anterior) {
				m = m.plus(1);
			}
			meses.add(mes(b, m));
			anterior = m;
		}
		return new LluviasEsperadas(fuente(p), meses);
	}

	static LluviaMensual mes(PlanCiclo.Bloque b, Month nombre) {
		String mes = nombreMes(nombre);
		int pct = b.anomaliaLluviaPct();
		if (pct <= -UMBRAL_PCT) {
			return new LluviaMensual(mes, b.lluviaMmP50(), "seco", "Se espera menos lluvia de lo normal ("
				+ b.lluviaMmP50() + " mm frente a " + b.lluviaNormalMm() + " mm): cuide el agua.");
		}
		if (pct >= UMBRAL_PCT) {
			return new LluviaMensual(mes, b.lluviaMmP50(), "lluvioso",
				"Más lluvia de lo normal: revise que el agua no se encharque.");
		}
		return new LluviaMensual(mes, b.lluviaMmP50(), "normal", null);
	}

	static String nombreMes(Month m) {
		String n = m.getDisplayName(java.time.format.TextStyle.FULL, ES);
		return n.substring(0, 1).toUpperCase(ES) + n.substring(1);
	}

	static String fuente(PlanCiclo.Pronostico p) {
		if (p == null) {
			return "NASA POWER";
		}
		if ("c3s".equals(p.fuente())) {
			return "ECMWF SEAS5 corregido con NASA POWER";
		}
		return "NASA POWER · años parecidos según El Niño (ONI " + String.format(ES, "%.1f", p.oni()) + ")";
	}

	/** La tabla histórica del plan, en la forma de la API pública (solo P(éxito) y riego). */
	public static HistoricoCultivos historico(PlanCiclo plan) {
		PlanCiclo.Historico h = plan.historico();
		if (h == null || h.fechas() == null) {
			return HistoricoCultivos.vacio();
		}
		return new HistoricoCultivos(h.fuente(), h.fechas(), columna(h, PlanCiclo.Serie::probExito),
			columna(h, PlanCiclo.Serie::riegoMmP50));
	}

	private static Map<String, List<Double>> columna(PlanCiclo.Historico h,
		Function<PlanCiclo.Serie, List<Double>> campo) {
		Map<String, List<Double>> m = new HashMap<>();
		h.cultivos().forEach((id, serie) -> m.put(id, new ArrayList<>(campo.apply(serie))));
		return m;
	}
}
