package com.nameless.nasa.infrastructure.input.rest.response;

public record AlertResponse(
        String tipo, String nivel, String zona, String mensaje, String fuente) {}