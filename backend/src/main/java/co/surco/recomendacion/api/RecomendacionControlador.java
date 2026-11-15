package co.surco.recomendacion.api;

import co.surco.compartido.seguridad.UsuarioAutenticado;
import co.surco.recomendacion.aplicacion.RecomendarCiclo;
import co.surco.recomendacion.dominio.PlanCiclo;
import co.surco.recomendacion.dominio.Puertos.TerrenoUbicado;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** El mejor ciclo de cultivo de un terreno del productor autenticado. Protegida (no está en RutasPublicas). */
@RestController
class RecomendacionControlador {

	private final RecomendarCiclo recomendar;

	RecomendacionControlador(RecomendarCiclo recomendar) {
		this.recomendar = recomendar;
	}

	/** {@code plan} es la respuesta del planificador; {@code terreno}, lo que se le preguntó. */
	record RespuestaRecomendacion(TerrenoUbicado terreno, PlanCiclo plan) {
	}

	@GetMapping("/api/v1/recomendacion/terrenos/{terrenoId}")
	RespuestaRecomendacion paraTerreno(@AuthenticationPrincipal UsuarioAutenticado usuario,
		@PathVariable UUID terrenoId) {
		var r = recomendar.para(usuario.id(), terrenoId);
		return new RespuestaRecomendacion(r.terreno(), r.plan());
	}
}
