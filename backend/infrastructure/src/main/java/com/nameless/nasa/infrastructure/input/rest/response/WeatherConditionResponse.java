package com.nameless.nasa.infrastructure.input.rest.response;

/**
 * Una tarjeta del tablero. valor es null cuando la fuente no tiene dato reciente.
 *
 * @param medido cuándo se tomó el dato (fecha y hora de la estación, o el día en datos de la NASA)
 */
public record WeatherConditionResponse(
        String variable, Double valor, String unidad, String detalle, String fuente, String medido) {}
