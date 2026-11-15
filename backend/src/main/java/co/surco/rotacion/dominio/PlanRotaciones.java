package co.surco.rotacion.dominio;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Las mejores rotaciones calculadas para un lote, con el ranking y la lluvia esperada de la recomendación
 * de la que salieron. Se guardan para que la elección del agricultor apunte a algo que no cambia.
 */
public record PlanRotaciones(UUID id, UUID usuarioId, UUID terrenoId, UUID recomendacionId, Instant creadoEn,
	String nombreTerreno, double areaHa, String motor, String fuenteHistorico, List<Rotacion> rotaciones,
	List<Candidatos.CultivoRanking> ranking, Candidatos.Lluvias lluvias) {

	public boolean tiene(String rotacionId) {
		return rotaciones.stream().anyMatch(r -> r.id().equals(rotacionId));
	}
}
