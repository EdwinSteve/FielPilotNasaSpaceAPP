package co.surco.clima.dominio;

import co.surco.clima.dominio.Puertos.AlertaRio;
import co.surco.clima.dominio.ResumenClima.Aviso;
import co.surco.clima.dominio.ResumenClima.Dia;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;

/**
 * Convierte los datos en avisos para el agricultor. Solo el de los ríos es una alerta oficial (IDEAM);
 * los umbrales de lluvia fuerte y de sequía son una propuesta del equipo, no un estándar.
 */
public final class Avisos {

	/** Lluvia de un día desde la que se avisa. */
	public static final double LLUVIA_FUERTE_MM = 30;
	static final double LLUVIA_MUY_FUERTE_MM = 60;
	/** Se avisa de lluvia fuerte solo si cae hoy o en los dos días siguientes. */
	static final int DIAS_DE_AVISO = 3;

	static final int DIAS_VENTANA_SEQUIA = 30;
	static final int DIAS_MINIMOS_SEQUIA = 25;
	/** Si lo normal para esos 30 días es menos que esto, es época seca habitual: no se avisa. */
	static final double NORMAL_MINIMO_MM = 20;
	/** El último dato de lluvia no puede ser más viejo que esto para avisar hoy. */
	static final int DIAS_MAXIMOS_DE_RETRASO = 10;

	private static final Locale ES = Locale.forLanguageTag("es-CO");

	private Avisos() {
	}

	/** Del más grave al menos grave. */
	public static List<Aviso> ordenar(List<Aviso> avisos) {
		return avisos.stream().sorted(Comparator.comparing(Aviso::nivel).reversed()).toList();
	}

	/** Vacío si el IDEAM manda un nivel que no conocemos: no se inventa una alerta. */
	public static Optional<Aviso> deRios(AlertaRio alerta) {
		String nivel = alerta.nivelIdeam() == null ? "" : alerta.nivelIdeam().trim().toUpperCase(Locale.ROOT);
		return switch (nivel) {
			case "ALERTA AMARILLA" -> Optional.of(rios(Nivel.AMARILLA, alerta, "Ríos y quebradas en vigilancia",
				"El IDEAM está vigilando los ríos y quebradas de su zona. Todavía no hay peligro, pero manténgase pendiente."));
			case "ALERTA NARANJA" -> Optional.of(rios(Nivel.NARANJA, alerta, "Los ríos y quebradas pueden crecer pronto",
				"Prepárese: aleje de la orilla animales, bombas y herramientas."));
			case "ALERTA ROJA" -> Optional.of(rios(Nivel.ROJA, alerta, "Riesgo alto de creciente o inundación",
				"Aléjese de ríos y quebradas y ponga a salvo a su familia y sus animales."));
			default -> Optional.empty();
		};
	}

	private static Aviso rios(Nivel nivel, AlertaRio alerta, String titulo, String mensaje) {
		return new Aviso("RIOS", nivel, titulo, mensaje, alerta.subzona(), "IDEAM", true);
	}

	/** Aviso por el día más lluvioso entre hoy y los dos días siguientes, si pasa el umbral. */
	public static Optional<Aviso> deLluviaFuerte(List<Dia> pronostico, String fuente) {
		return pronostico.stream().limit(DIAS_DE_AVISO)
			.filter(d -> d.lluviaMm() >= LLUVIA_FUERTE_MM)
			.max(Comparator.comparingDouble(Dia::lluviaMm))
			.map(d -> {
				boolean hoy = d.equals(pronostico.get(0));
				String cuando = hoy ? "hoy" : "el " + d.fecha().getDayOfWeek().getDisplayName(TextStyle.FULL, ES);
				return new Aviso("LLUVIA_FUERTE", d.lluviaMm() >= LLUVIA_MUY_FUERTE_MM ? Nivel.NARANJA : Nivel.AMARILLA,
					"Lluvia fuerte " + cuando,
					String.format(ES, "Se esperan cerca de %.0f mm de lluvia. El suelo puede quedar encharcado: "
						+ "revise los drenajes y evite sembrar o abonar ese día.", d.lluviaMm()),
					null, fuente, false);
			});
	}

	/**
	 * Compara la lluvia de los últimos 30 días con la lluvia normal de esos mismos días.
	 * Vacío si llovió lo normal, si es época seca habitual o si no hay datos suficientes.
	 */
	public static Optional<Aviso> deSequia(SortedMap<LocalDate, Double> lluviaDiaria, Map<Month, Double> normalMmDia,
		LocalDate hoy, String fuente) {
		if (lluviaDiaria.isEmpty() || lluviaDiaria.lastKey().isBefore(hoy.minusDays(DIAS_MAXIMOS_DE_RETRASO))) {
			return Optional.empty();
		}
		LocalDate desde = lluviaDiaria.lastKey().minusDays(DIAS_VENTANA_SEQUIA - 1L);
		double lluvia = 0;
		double normal = 0;
		int dias = 0;
		for (Map.Entry<LocalDate, Double> dia : lluviaDiaria.tailMap(desde).entrySet()) {
			Double normalDelMes = normalMmDia.get(dia.getKey().getMonth());
			if (normalDelMes == null) {
				return Optional.empty();
			}
			lluvia += dia.getValue();
			normal += normalDelMes; // solo se suman los días que sí tienen dato
			dias++;
		}
		if (dias < DIAS_MINIMOS_SEQUIA || normal < NORMAL_MINIMO_MM) {
			return Optional.empty();
		}
		double porcentaje = lluvia / normal * 100;
		if (porcentaje >= 80) {
			return Optional.empty();
		}
		String comparacion = String.format(ES, "En el último mes llovieron %.0f mm y lo normal son %.0f mm. ", lluvia, normal);
		if (porcentaje >= 60) {
			return Optional.of(sequia(Nivel.AMARILLA, "Ha llovido menos de lo normal",
				comparacion + "Cuide el agua y revise la humedad de su suelo.", fuente));
		}
		if (porcentaje >= 40) {
			return Optional.of(sequia(Nivel.NARANJA, "Ha llovido mucho menos de lo normal",
				comparacion + "Riegue solo lo necesario y cubra el suelo para que no pierda humedad.", fuente));
		}
		return Optional.of(sequia(Nivel.ROJA, "Sequía fuerte",
			comparacion + "Asegure agua para sus animales y, si puede, aplace las siembras nuevas.", fuente));
	}

	private static Aviso sequia(Nivel nivel, String titulo, String mensaje, String fuente) {
		return new Aviso("SEQUIA", nivel, titulo, mensaje, null, fuente, false);
	}
}
