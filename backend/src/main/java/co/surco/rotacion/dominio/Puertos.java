package co.surco.rotacion.dominio;

import java.util.Optional;
import java.util.UUID;

/** Puertos de la rotación. Las pruebas los reemplazan por versiones en memoria. */
public final class Puertos {

	private Puertos() {
	}

	/** La recomendación del terreno (módulo recomendacion). Lanza sus errores: 404, 422, 502, 503. */
	public interface Recomendaciones {
		Candidatos calcular(UUID usuarioId, UUID terrenoId);
	}

	public interface RepositorioPlanes {
		void guardar(PlanRotaciones plan);

		Optional<PlanRotaciones> ultimo(UUID usuarioId, UUID terrenoId);

		Optional<PlanRotaciones> porId(UUID usuarioId, UUID terrenoId, UUID planId);
	}

	public interface RepositorioSelecciones {
		/** Guarda o reemplaza la selección del lote. */
		void guardar(Seleccion seleccion);

		Optional<Seleccion> de(UUID usuarioId, UUID terrenoId);
	}
}
