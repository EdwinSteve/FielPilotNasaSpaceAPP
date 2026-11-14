package com.nameless.nasa.domain.model.enums;

import java.util.Locale;

public enum AlertMessages {
    NORMAL("CONDICIONES NORMALES",
            "Sin alertas por ahora en los ríos y quebradas de tu zona."),
    AMARILLA("ALERTA AMARILLA",
            "Atención: el IDEAM está vigilando los ríos y quebradas de tu zona. "
                    + "Todavía no hay peligro, pero mantente pendiente."),
    NARANJA("ALERTA NARANJA",
            "Prepárate: los ríos y quebradas de tu zona pueden crecer pronto. "
                    + "Aleja de la orilla animales, bombas y herramientas."),
    ROJA("ALERTA ROJA",
            "Peligro: hay riesgo alto de creciente o inundación en tu zona. "
                    + "Aléjate de ríos y quebradas y pon a salvo a tu familia y tus animales."),
    SIN_ALERTA(null,
            "El IDEAM no tiene alertas vigentes para los ríos y quebradas de tu zona.");

    private final String valorIdeam;
    private final String mensaje;

    AlertMessages(String valorIdem, String mensaje) {
        this.valorIdeam = valorIdem;
        this.mensaje = mensaje;
    }

    public String message() {
        return mensaje;
    }

    /**
     * @throws IllegalArgumentException si el IDEAM manda un valor que no conocemos.
     *                                  No se asume NORMAL: sería esconder una alerta.
     */
    public static AlertMessages desdeIdeam(String nivelA) {
        String limpio = nivelA == null ? "" : nivelA.trim().toUpperCase(Locale.ROOT);
        for (AlertMessages nivel : values()) {
            if (limpio.equals(nivel.valorIdeam)) {
                return nivel;
            }
        }
        throw new IllegalArgumentException("Nivel del IDEAM no reconocido: " + nivelA);
    }
}
