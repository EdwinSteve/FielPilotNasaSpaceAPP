package co.surco.recomendacion.dominio;

import java.util.Optional;
import java.util.UUID;

/**
 * Puertos que la recomendación necesita del exterior: el servicio del modelo (HTTP) y los terrenos del
 * productor (módulo fincas). Las pruebas los reemplazan por versiones en memoria.
 */
public final class Puertos {

	private Puertos() {
	}

	/** El planificador estacional: trae el clima del punto y corre la red. Puede tardar (1-2 min la primera vez). */
	public interface MotorPlanificador {
		PlanCiclo planificar(double lat, double lon, boolean riego);
	}

	/** Dónde queda el terreno del productor. Vacío si no existe o es de otro productor. */
	public interface TerrenosDelProductor {
		Optional<TerrenoUbicado> de(UUID usuarioId, UUID terrenoId);
	}

	public record TerrenoUbicado(UUID id, String nombre, String municipio, double lat, double lon, double areaHa,
		boolean riego) {
	}
}
