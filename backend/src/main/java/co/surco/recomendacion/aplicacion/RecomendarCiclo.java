package co.surco.recomendacion.aplicacion;

import co.surco.recomendacion.dominio.ErroresRecomendacion;
import co.surco.recomendacion.dominio.PlanCiclo;
import co.surco.recomendacion.dominio.Puertos.MotorPlanificador;
import co.surco.recomendacion.dominio.Puertos.TerrenoUbicado;
import co.surco.recomendacion.dominio.Puertos.TerrenosDelProductor;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * El mejor ciclo de cultivo para un terreno del productor: toma su centroide y si tiene riego
 * (módulo fincas) y se lo pasa al planificador estacional, que trae el clima esperado y corre la red.
 * Sin transacción: no escribe en la base y la llamada al modelo puede tardar.
 */
@Service
public class RecomendarCiclo {

	private final TerrenosDelProductor terrenos;
	private final MotorPlanificador motor;

	public RecomendarCiclo(TerrenosDelProductor terrenos, MotorPlanificador motor) {
		this.terrenos = terrenos;
		this.motor = motor;
	}

	public record Recomendacion(TerrenoUbicado terreno, PlanCiclo plan) {
	}

	public Recomendacion para(UUID usuarioId, UUID terrenoId) {
		TerrenoUbicado t = terrenos.de(usuarioId, terrenoId).orElseThrow(ErroresRecomendacion::terrenoNoEncontrado);
		return new Recomendacion(t, motor.planificar(t.lat(), t.lon(), t.riego()));
	}
}
