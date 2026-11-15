package co.surco.rotacion.infraestructura.recomendacion;

import co.surco.rotacion.dominio.Candidatos;

/** Candidatos de una respuesta real del planificador (Neiva, Bf), para las pruebas de rotacion. */
public final class CandidatosDePrueba {

	private CandidatosDePrueba() {
	}

	public static Candidatos neiva() {
		return RotacionesConPlanRealTest.neiva();
	}
}
