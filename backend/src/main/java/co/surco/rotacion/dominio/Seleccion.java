package co.surco.rotacion.dominio;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** El ciclo que eligió el agricultor: el plan activo del lote. Uno por lote; elegir otro lo reemplaza. */
public record Seleccion(UUID terrenoId, UUID usuarioId, UUID planId, String rotacionId, List<String> secuencia,
	Instant elegidaEn) {
}
