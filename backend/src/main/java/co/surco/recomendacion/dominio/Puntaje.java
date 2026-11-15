package co.surco.recomendacion.dominio;

import co.surco.recomendacion.ConsultaRecomendaciones.CultivoEvaluado;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * El ranking de cultivos que ve el agricultor ("qué tanto le conviene", de 0 a 1).
 *
 * <p>Mapeo del puntaje (documentado también en docs/decisiones/0008-recomendacion-y-rotacion.md):
 * <ul>
 *   <li>Terreno en secano: {@code puntaje = (prob_exito + rend_p50) / 2}. Premia por igual que el cultivo
 *   salga bien casi todos los años (P de lograr el 60 % del potencial) y cuánto rinde en el año típico.</li>
 *   <li>Terreno con riego: {@code puntaje = rend_con_riego_p50}. Con riego el agua deja de limitar, así
 *   que lo que distingue a los cultivos es cuánto rinden en el año típico regándolos.</li>
 * </ul>
 * Se recorta a [0, 1]. Los filtros duros (altitud y pH del suelo) los aplica el planificador con las
 * fichas del catálogo; un cultivo no apto queda al final con su razón, sin importar su puntaje.
 */
public final class Puntaje {

	private Puntaje() {
	}

	public static double de(PlanCiclo.Cultivo c, boolean riego) {
		double p = riego ? c.rendConRiegoP50() : (c.probExito() + c.rendP50()) / 2;
		return Math.round(Math.clamp(p, 0, 1) * 1000) / 1000.0;
	}

	/**
	 * Los cultivos de la fecha de siembra recomendada (la del mejor ciclo; si no hay, la primera que se
	 * comparó): los aptos primero, de mayor a menor puntaje, y luego los no aptos.
	 */
	public static List<CultivoEvaluado> ranking(PlanCiclo plan) {
		if (plan.opciones() == null || plan.opciones().isEmpty()) {
			return List.of();
		}
		boolean riego = plan.lote() != null && plan.lote().riego();
		LocalDate siembra = plan.mejor() != null ? plan.mejor().siembra() : plan.opciones().getFirst().siembra();
		PlanCiclo.Opcion opcion = plan.opciones().stream().filter(o -> o.siembra().equals(siembra)).findFirst()
			.orElse(plan.opciones().getFirst());
		return opcion.cultivos().stream()
			.map(c -> new CultivoEvaluado(c.cultivo(), c.nombre(), c.familia(), c.fija(), c.fijacionNitrogenoKgHa(),
				c.cicloDias(), opcion.siembra(), c.cosecha(), de(c, riego), c.probExito(), c.riegoMmP50(), c.apto(),
				c.razonNoApto()))
			.sorted(Comparator.comparing(CultivoEvaluado::apto).reversed()
				.thenComparing(Comparator.comparingDouble(CultivoEvaluado::puntaje).reversed()))
			.toList();
	}
}
