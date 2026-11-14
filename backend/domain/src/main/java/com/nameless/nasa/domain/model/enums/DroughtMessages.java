package com.nameless.nasa.domain.model.enums;

/**
 * Convierte "cuánto ha llovido frente a lo normal" (en porcentaje) en un nivel
 * y un mensaje claro para el agricultor.
 *
 * Los umbrales son una propuesta del equipo, no un estándar oficial.
 */
public enum DroughtMessages {
    NORMAL(80,
            "La lluvia del último mes ha sido normal en tu zona."),
    AMARILLA(60,
            "Ha llovido menos de lo normal en el último mes. "
                    + "Cuida el agua y revisa la humedad de tu suelo."),
    NARANJA(40,
            "Ha llovido mucho menos de lo normal en el último mes. "
                    + "Riega solo lo necesario y cubre el suelo para que no pierda humedad."),
    ROJA(0,
            "Sequía fuerte: casi no ha llovido en el último mes. "
                    + "Asegura agua para tus animales y, si puedes, aplaza las siembras nuevas.");

    private final double desdePorcentaje;
    private final String mensaje;

    DroughtMessages(double desdePorcentaje, String mensaje) {
        this.desdePorcentaje = desdePorcentaje;
        this.mensaje = mensaje;
    }

    public String message() {
        return mensaje;
    }

    /** 100 = llovió lo normal; 50 = llovió la mitad de lo normal. */
    public static DroughtMessages desdePorcentaje(double porcentaje) {
        for (DroughtMessages nivel : values()) {
            if (porcentaje >= nivel.desdePorcentaje) {
                return nivel;
            }
        }
        return ROJA;
    }
}
