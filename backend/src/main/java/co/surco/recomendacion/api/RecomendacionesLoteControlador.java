package co.surco.recomendacion.api;

import co.surco.compartido.seguridad.UsuarioAutenticado;
import co.surco.recomendacion.ConsultaRecomendaciones.CultivoEvaluado;
import co.surco.recomendacion.ConsultaRecomendaciones.LluviasEsperadas;
import co.surco.recomendacion.aplicacion.CalcularRecomendacion;
import co.surco.recomendacion.aplicacion.CalcularRecomendacion.Calculada;
import co.surco.recomendacion.dominio.PlanCiclo;
import co.surco.recomendacion.dominio.Recomendacion;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recomendaciones de cultivo de un lote (terreno) del productor autenticado, calculadas y guardadas.
 * Protegidas (no están en RutasPublicas). Un terreno ajeno responde 404 igual que uno inexistente.
 *
 * <ul>
 *   <li>{@code POST /api/v1/lotes/{id}/recomendaciones?siembra=AAAA-MM-DD}: calcula con el planificador y
 *   guarda (201). Sin {@code siembra} compara las fechas del próximo mes. Puede tardar 1-3 minutos la
 *   primera vez en una zona.</li>
 *   <li>{@code GET /api/v1/lotes/{id}/recomendaciones/actual}: la más reciente (404 si no hay).</li>
 *   <li>{@code GET /api/v1/lotes/{id}/recomendaciones/{recId}}: una guardada.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/lotes/{loteId}/recomendaciones")
class RecomendacionesLoteControlador {

	private final CalcularRecomendacion recomendaciones;

	RecomendacionesLoteControlador(CalcularRecomendacion recomendaciones) {
		this.recomendaciones = recomendaciones;
	}

	/**
	 * Espejo de {@code frontend/src/modulos/recomendacion/tipos.ts} ({@code RecomendacionLote}).
	 *
	 * @param ranking los cultivos que puntúa el modelo (aptos primero, por puntaje 0-1)
	 * @param mejor   el cultivo apto con mejor rendimiento esperado y cuándo sembrarlo; null si ninguno es apto
	 */
	record RespuestaRecomendacion(UUID id, UUID terrenoId, Instant creadaEn, LocalDate siembraPedida, String motor,
		String modelo, double elevacionM, double phSuelo, boolean riego, Mejor mejor, List<CultivoEvaluado> ranking,
		LluviasEsperadas lluvias) {

		record Mejor(String cultivo, String nombre, LocalDate siembra) {
		}

		static RespuestaRecomendacion de(Calculada c) {
			Recomendacion r = c.recomendacion();
			PlanCiclo plan = r.plan();
			var publica = CalcularRecomendacion.publica(c);
			Mejor mejor = plan.mejor() == null ? null
				: new Mejor(plan.mejor().cultivo().cultivo(), plan.mejor().cultivo().nombre(), plan.mejor().siembra());
			return new RespuestaRecomendacion(r.id(), r.terrenoId(), r.creadaEn(), r.siembraPedida(), publica.motor(),
				plan.modelo(), plan.lote().elevacionM(), plan.lote().phSuelo(), c.terreno().riego(), mejor,
				publica.cultivos(), publica.lluvias());
		}
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	RespuestaRecomendacion calcular(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID loteId,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate siembra) {
		return RespuestaRecomendacion.de(recomendaciones.ejecutar(usuario.id(), loteId, siembra));
	}

	@GetMapping("/actual")
	RespuestaRecomendacion actual(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID loteId) {
		return RespuestaRecomendacion.de(recomendaciones.ultima(usuario.id(), loteId));
	}

	@GetMapping("/{recomendacionId}")
	RespuestaRecomendacion una(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID loteId,
		@PathVariable UUID recomendacionId) {
		return RespuestaRecomendacion.de(recomendaciones.obtener(usuario.id(), loteId, recomendacionId));
	}
}
