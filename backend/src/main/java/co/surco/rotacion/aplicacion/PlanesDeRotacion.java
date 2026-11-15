package co.surco.rotacion.aplicacion;

import co.surco.rotacion.dominio.Candidatos;
import co.surco.rotacion.dominio.ErroresRotacion;
import co.surco.rotacion.dominio.GeneradorRotaciones;
import co.surco.rotacion.dominio.PlanRotaciones;
import co.surco.rotacion.dominio.Puertos.Recomendaciones;
import co.surco.rotacion.dominio.Puertos.RepositorioPlanes;
import co.surco.rotacion.dominio.Puertos.RepositorioSelecciones;
import co.surco.rotacion.dominio.Rotacion;
import co.surco.rotacion.dominio.Seleccion;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Casos de uso de la rotación de un lote: calcular las 3 mejores (con la recomendación del modelo),
 * leer el plan guardado y guardar el ciclo que elige el agricultor.
 *
 * <p>Sin transacción alrededor del cálculo: la recomendación puede tardar minutos. Los repositorios
 * guardan cada uno en su propia transacción.
 */
@Service
public class PlanesDeRotacion {

	private final Recomendaciones recomendaciones;
	private final RepositorioPlanes planes;
	private final RepositorioSelecciones selecciones;
	private final Clock reloj;

	public PlanesDeRotacion(Recomendaciones recomendaciones, RepositorioPlanes planes,
		RepositorioSelecciones selecciones, Clock reloj) {
		this.recomendaciones = recomendaciones;
		this.planes = planes;
		this.selecciones = selecciones;
		this.reloj = reloj;
	}

	/** El plan con su selección, si el agricultor ya eligió un ciclo de ese mismo plan o de uno anterior. */
	public record PlanConSeleccion(PlanRotaciones plan, Optional<Seleccion> seleccion) {
	}

	/** Pide la recomendación del lote (404 si es ajeno), arma las rotaciones y guarda el plan. */
	public PlanConSeleccion calcular(UUID usuarioId, UUID terrenoId) {
		Candidatos c = recomendaciones.calcular(usuarioId, terrenoId);
		List<Rotacion> mejores = GeneradorRotaciones.mejores(c);
		var plan = new PlanRotaciones(UUID.randomUUID(), usuarioId, terrenoId, c.recomendacionId(), reloj.instant(),
			c.nombreTerreno(), c.areaHa(), c.motor(), c.historico() == null ? "" : c.historico().fuente(), mejores,
			c.ranking(), c.lluvias());
		planes.guardar(plan);
		return new PlanConSeleccion(plan, selecciones.de(usuarioId, terrenoId));
	}

	/** El último plan calculado del lote. 404 ROTACION_SIN_CALCULAR si no hay (o el lote es ajeno). */
	public PlanConSeleccion ultimo(UUID usuarioId, UUID terrenoId) {
		PlanRotaciones plan = planes.ultimo(usuarioId, terrenoId).orElseThrow(ErroresRotacion::sinCalcular);
		return new PlanConSeleccion(plan, selecciones.de(usuarioId, terrenoId));
	}

	/** Guarda (o reemplaza) el ciclo elegido como plan activo del lote. */
	public Seleccion elegir(UUID usuarioId, UUID terrenoId, UUID planId, String rotacionId) {
		PlanRotaciones plan = planes.porId(usuarioId, terrenoId, planId).orElseThrow(ErroresRotacion::sinCalcular);
		Rotacion r = plan.rotaciones().stream().filter(x -> x.id().equals(rotacionId)).findFirst()
			.orElseThrow(ErroresRotacion::rotacionNoEncontrada);
		var s = new Seleccion(terrenoId, usuarioId, planId, r.id(), r.secuencia(), reloj.instant());
		selecciones.guardar(s);
		return s;
	}

	public Seleccion seleccion(UUID usuarioId, UUID terrenoId) {
		return selecciones.de(usuarioId, terrenoId).orElseThrow(ErroresRotacion::sinSeleccion);
	}
}
