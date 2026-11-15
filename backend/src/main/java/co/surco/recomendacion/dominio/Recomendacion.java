package co.surco.recomendacion.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Una recomendación calculada para un terreno: la respuesta completa del planificador, guardada para
 * poder volver a mostrarla sin repetir el cálculo y para saber de dónde salió cada rotación.
 *
 * @param siembraPedida la fecha que pidió el productor; null si se compararon las del próximo mes
 */
public record Recomendacion(UUID id, UUID usuarioId, UUID terrenoId, Instant creadaEn, LocalDate siembraPedida,
	PlanCiclo plan) {
}
