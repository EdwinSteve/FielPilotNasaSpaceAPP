/**
 * Módulo rotacion: los ciclos productivos de un lote en secuencias de 2-3 temporadas y el que elige el
 * agricultor (docs/decisiones/0008-recomendacion-y-rotacion.md).
 *
 * <p>Rutas (protegidas, no están en RutasPublicas):
 * <ul>
 *   <li>{@code POST /api/v1/lotes/{id}/rotaciones}: pide la recomendación del lote al módulo recomendacion
 *   (por su API pública), arma las 3 mejores rotaciones y guarda el plan (esquema "rotacion", V6).</li>
 *   <li>{@code GET /api/v1/lotes/{id}/rotaciones}: el último plan, con el ranking, la lluvia esperada y la
 *   selección del agricultor.</li>
 *   <li>{@code PUT /api/v1/lotes/{id}/seleccion {plan_id, rotacion_id}} y {@code GET}: el ciclo elegido,
 *   plan activo del lote (uno por lote; elegir otro lo reemplaza).</li>
 * </ul>
 *
 * <p>El algoritmo v1 está en {@code dominio.GeneradorRotaciones}: primera temporada con el pronóstico,
 * siguientes con la frecuencia de éxito en años pasados, sin repetir familia botánica seguida, bono por
 * leguminosa, grado por umbrales y agua ahorrada frente al monocultivo comercial de referencia.
 *
 * <p>Fuera de alcance por ahora: el pronóstico IDEAM de días, la salud del suelo por temporada y el
 * simulador de escenarios (+°C, lluvia).
 *
 * <p>Estructura, igual que el módulo identidad:
 * <ul>
 *   <li>{@code dominio}: Candidatos, GeneradorRotaciones, Rotacion, PlanRotaciones, Seleccion, puertos y
 *   errores. Java puro.</li>
 *   <li>{@code aplicacion}: PlanesDeRotacion.</li>
 *   <li>{@code infraestructura}: RecomendacionesDelModelo (API pública de recomendacion) y persistencia (JPA).</li>
 *   <li>{@code api}: RotacionesControlador.</li>
 * </ul>
 */
package co.surco.rotacion;
