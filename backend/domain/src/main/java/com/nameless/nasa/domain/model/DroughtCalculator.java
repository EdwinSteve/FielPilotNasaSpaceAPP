package com.nameless.nasa.domain.model;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import java.util.SortedMap;

/**
 * Compara la lluvia de los últimos 30 días con la lluvia normal de esos mismos días.
 * Java puro: no depende de Spring ni de la red.
 */
public final class DroughtCalculator {

    public static final int DIAS_VENTANA = 30;
    private static final int DIAS_MINIMOS = 25;

    /**
     * Si lo normal para esos 30 días es menos que esto, es época seca habitual:
     * el porcentaje se dispara con cualquier diferencia y daría falsas alarmas.
     */
    private static final double NORMAL_MINIMO_MM = 20.0;

    private DroughtCalculator() {}

    public record Resultado(
            double lluviaMm, double normalMm, double porcentaje, LocalDate hasta, boolean epocaSeca) {}

    /**
     * @param lluviaDiaria mm por día, solo días con dato válido
     * @param normalMmDia  lluvia normal en mm/día para cada mes
     * @throws IllegalStateException si no hay datos suficientes para calcular
     */
    public static Resultado calcular(
            SortedMap<LocalDate, Double> lluviaDiaria, Map<Month, Double> normalMmDia) {
        if (lluviaDiaria.isEmpty()) {
            throw new IllegalStateException("No hay datos de lluvia para esa ubicación");
        }
        LocalDate hasta = lluviaDiaria.lastKey();
        LocalDate desde = hasta.minusDays(DIAS_VENTANA - 1);

        double lluvia = 0;
        double normal = 0;
        int dias = 0;
        for (Map.Entry<LocalDate, Double> dia : lluviaDiaria.tailMap(desde).entrySet()) {
            Double normalDelMes = normalMmDia.get(dia.getKey().getMonth());
            if (normalDelMes == null) {
                throw new IllegalStateException("Falta la lluvia normal de " + dia.getKey().getMonth());
            }
            lluvia += dia.getValue();
            normal += normalDelMes;   // solo se suman los días que sí tienen dato
            dias++;
        }
        if (dias < DIAS_MINIMOS) {
            throw new IllegalStateException("Solo hay " + dias + " días con dato en la ventana");
        }

        boolean epocaSeca = normal < NORMAL_MINIMO_MM;
        double porcentaje = epocaSeca ? 100.0 : lluvia / normal * 100.0;
        return new Resultado(lluvia, normal, porcentaje, hasta, epocaSeca);
    }
}
