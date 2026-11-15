package co.surco.fincas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * API pública del módulo fincas: dónde quedan los terrenos de un productor. La usan diagnóstico,
 * recomendación (el planificador estacional recibe el centroide) y alertas. Otros módulos solo usan
 * esta interfaz y los records de este paquete, nunca las subcapas.
 */
public interface ConsultaTerrenos {

	/** Los terrenos del productor, del más antiguo al más nuevo. */
	List<UbicacionTerreno> terrenosDe(UUID usuarioId);

	/** Vacío si no existe o es de otro productor. */
	Optional<UbicacionTerreno> terreno(UUID usuarioId, UUID terrenoId);
}
