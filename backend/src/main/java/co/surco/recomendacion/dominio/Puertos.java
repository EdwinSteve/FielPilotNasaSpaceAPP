package co.surco.recomendacion.dominio;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Puertos que la recomendación necesita del exterior: el servicio del modelo (HTTP), los terrenos del
 * productor (módulo fincas) y dónde guardar lo calculado. Las pruebas los reemplazan por versiones en memoria.
 */
public final class Puertos {

	private Puertos() {
	}

	/**
	 * El planificador estacional: trae el clima del punto y puntúa los cultivos. Puede tardar (1-3 min la
	 * primera vez en una zona). {@code siembra} null compara las fechas de siembra del próximo mes.
	 */
	public interface MotorPlanificador {
		PlanCiclo planificar(double lat, double lon, boolean riego, LocalDate siembra);
	}

	/** Las recomendaciones calculadas, en el esquema "recomendacion". */
	public interface RepositorioRecomendaciones {
		void guardar(Recomendacion recomendacion);

		/** Vacío si no existe, es de otro terreno o de otro productor. */
		Optional<Recomendacion> porId(UUID usuarioId, UUID terrenoId, UUID id);

		/** La más reciente del terreno. */
		Optional<Recomendacion> ultima(UUID usuarioId, UUID terrenoId);
	}

	/** Dónde queda el terreno del productor. Vacío si no existe o es de otro productor. */
	public interface TerrenosDelProductor {
		Optional<TerrenoUbicado> de(UUID usuarioId, UUID terrenoId);
	}

	public record TerrenoUbicado(UUID id, String nombre, String municipio, double lat, double lon, double areaHa,
		boolean riego) {
	}
}
