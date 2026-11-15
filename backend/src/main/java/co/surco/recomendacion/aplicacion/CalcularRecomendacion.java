package co.surco.recomendacion.aplicacion;

import co.surco.recomendacion.ConsultaRecomendaciones;
import co.surco.recomendacion.dominio.ErroresRecomendacion;
import co.surco.recomendacion.dominio.Lluvias;
import co.surco.recomendacion.dominio.PlanCiclo;
import co.surco.recomendacion.dominio.Puertos.MotorPlanificador;
import co.surco.recomendacion.dominio.Puertos.RepositorioRecomendaciones;
import co.surco.recomendacion.dominio.Puertos.TerrenoUbicado;
import co.surco.recomendacion.dominio.Puertos.TerrenosDelProductor;
import co.surco.recomendacion.dominio.Puntaje;
import co.surco.recomendacion.dominio.Recomendacion;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Calcula la recomendación de un terreno del productor con el planificador estacional y la guarda
 * (esquema "recomendacion"). Implementa también la API pública que usa rotacion.
 *
 * <p>Sin transacción alrededor de la llamada al modelo: puede tardar minutos y no debe tener una
 * conexión de la base abierta mientras tanto. El repositorio guarda en su propia transacción.
 */
@Service
public class CalcularRecomendacion implements ConsultaRecomendaciones {

	/** Las fechas se cuentan en hora de Colombia, como las ve el agricultor. */
	static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
	static final int MAX_DIAS_SIEMBRA = 60;

	private final TerrenosDelProductor terrenos;
	private final MotorPlanificador motor;
	private final RepositorioRecomendaciones repositorio;
	private final Clock reloj;

	public CalcularRecomendacion(TerrenosDelProductor terrenos, MotorPlanificador motor,
		RepositorioRecomendaciones repositorio, Clock reloj) {
		this.terrenos = terrenos;
		this.motor = motor;
		this.repositorio = repositorio;
		this.reloj = reloj;
	}

	public record Calculada(TerrenoUbicado terreno, Recomendacion recomendacion) {
	}

	/** @param siembra null compara las fechas de siembra del próximo mes (lo normal desde la app) */
	public Calculada ejecutar(UUID usuarioId, UUID terrenoId, LocalDate siembra) {
		TerrenoUbicado t = terreno(usuarioId, terrenoId);
		LocalDate hoy = LocalDate.now(reloj.withZone(COLOMBIA));
		if (siembra != null && (siembra.isBefore(hoy) || siembra.isAfter(hoy.plusDays(MAX_DIAS_SIEMBRA)))) {
			throw ErroresRecomendacion.siembraInvalida();
		}
		PlanCiclo plan = motor.planificar(t.lat(), t.lon(), t.riego(), siembra);
		var r = new Recomendacion(UUID.randomUUID(), usuarioId, terrenoId, reloj.instant(), siembra, plan);
		repositorio.guardar(r);
		return new Calculada(t, r);
	}

	public Calculada obtener(UUID usuarioId, UUID terrenoId, UUID id) {
		TerrenoUbicado t = terreno(usuarioId, terrenoId);
		return new Calculada(t, repositorio.porId(usuarioId, terrenoId, id)
			.orElseThrow(ErroresRecomendacion::recomendacionNoEncontrada));
	}

	public Calculada ultima(UUID usuarioId, UUID terrenoId) {
		TerrenoUbicado t = terreno(usuarioId, terrenoId);
		return new Calculada(t, repositorio.ultima(usuarioId, terrenoId)
			.orElseThrow(ErroresRecomendacion::recomendacionNoEncontrada));
	}

	@Override
	public RecomendacionTerreno calcular(UUID usuarioId, UUID terrenoId) {
		return publica(ejecutar(usuarioId, terrenoId, null));
	}

	public static RecomendacionTerreno publica(Calculada c) {
		Recomendacion r = c.recomendacion();
		PlanCiclo plan = r.plan();
		String motorUsado = plan.motor() == null ? "red" : plan.motor().id();
		return new RecomendacionTerreno(r.id(), r.terrenoId(), c.terreno().nombre(), c.terreno().areaHa(),
			c.terreno().riego(), r.creadaEn(), motorUsado, Puntaje.ranking(plan), Lluvias.historico(plan),
			Lluvias.de(plan));
	}

	private TerrenoUbicado terreno(UUID usuarioId, UUID terrenoId) {
		return terrenos.de(usuarioId, terrenoId).orElseThrow(ErroresRecomendacion::terrenoNoEncontrado);
	}
}
