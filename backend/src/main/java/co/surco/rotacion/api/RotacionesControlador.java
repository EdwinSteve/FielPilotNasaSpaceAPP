package co.surco.rotacion.api;

import co.surco.compartido.seguridad.UsuarioAutenticado;
import co.surco.rotacion.aplicacion.PlanesDeRotacion;
import co.surco.rotacion.aplicacion.PlanesDeRotacion.PlanConSeleccion;
import co.surco.rotacion.dominio.Candidatos;
import co.surco.rotacion.dominio.PlanRotaciones;
import co.surco.rotacion.dominio.Rotacion;
import co.surco.rotacion.dominio.Seleccion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotaciones de un lote del productor autenticado. Protegidas (no están en RutasPublicas).
 *
 * <ul>
 *   <li>{@code POST /api/v1/lotes/{id}/rotaciones}: pide la recomendación al modelo, arma las 3 mejores
 *   rotaciones y guarda el plan (201). Puede tardar 1-3 minutos la primera vez en una zona.</li>
 *   <li>{@code GET /api/v1/lotes/{id}/rotaciones}: el último plan (404 ROTACION_SIN_CALCULAR si no hay).</li>
 *   <li>{@code PUT /api/v1/lotes/{id}/seleccion {plan_id, rotacion_id}}: el ciclo elegido, plan activo del lote.</li>
 *   <li>{@code GET /api/v1/lotes/{id}/seleccion}: el ciclo elegido (404 ROTACION_SIN_SELECCION).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/lotes/{loteId}")
class RotacionesControlador {

	private static final Locale ES = Locale.of("es", "CO");

	private final PlanesDeRotacion planes;

	RotacionesControlador(PlanesDeRotacion planes) {
		this.planes = planes;
	}

	/** Espejo de {@code frontend/src/modulos/rotacion/tipos.ts} (RespuestaPlanRotacion). */
	record RespuestaPlan(UUID id, UUID terrenoId, UUID recomendacionId, Instant creadoEn, String nombreTerreno,
		double areaHa, String motor, String fuenteHistorico, List<RotacionDto> rotaciones,
		List<Candidatos.CultivoRanking> ranking, Candidatos.Lluvias lluvias, RespuestaSeleccion seleccion) {

		static RespuestaPlan de(PlanConSeleccion ps) {
			PlanRotaciones p = ps.plan();
			return new RespuestaPlan(p.id(), p.terrenoId(), p.recomendacionId(), p.creadoEn(), p.nombreTerreno(),
				p.areaHa(), p.motor(), p.fuenteHistorico(), p.rotaciones().stream().map(RotacionDto::de).toList(),
				p.ranking(), p.lluvias(), ps.seleccion().map(RespuestaSeleccion::de).orElse(null));
		}
	}

	/**
	 * @param aguaAhorrada ya formateada para mostrar ("2.100 m³")
	 */
	record RotacionDto(String id, String grado, List<String> secuencia, List<TemporadaDto> temporadas,
		double puntaje, int frecuenciaExito, String aguaAhorrada, long aguaAhorradaM3, String aporteNitrogeno) {

		static RotacionDto de(Rotacion r) {
			return new RotacionDto(r.id(), r.grado(), r.secuencia(), r.temporadas().stream().map(TemporadaDto::de)
				.toList(), r.puntaje(), r.frecuenciaExito(), NumberFormat.getIntegerInstance(ES)
					.format(r.aguaAhorradaM3()) + " m³", r.aguaAhorradaM3(), r.aporteNitrogeno());
		}
	}

	record TemporadaDto(String cultivo, String nombre, LocalDate siembra, LocalDate cosecha, double probExito,
		String fuente) {

		static TemporadaDto de(Rotacion.Temporada t) {
			return new TemporadaDto(t.cultivo(), t.nombre(), t.siembra(), t.cosecha(), t.probExito(), t.fuente());
		}
	}

	record RespuestaSeleccion(UUID planId, String rotacionId, List<String> secuencia, Instant elegidaEn) {

		static RespuestaSeleccion de(Seleccion s) {
			return new RespuestaSeleccion(s.planId(), s.rotacionId(), s.secuencia(), s.elegidaEn());
		}
	}

	record SolicitudSeleccion(@NotNull UUID planId, @NotBlank String rotacionId) {
	}

	@PostMapping("/rotaciones")
	@ResponseStatus(HttpStatus.CREATED)
	RespuestaPlan calcular(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID loteId) {
		return RespuestaPlan.de(planes.calcular(usuario.id(), loteId));
	}

	@GetMapping("/rotaciones")
	RespuestaPlan ultimo(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID loteId) {
		return RespuestaPlan.de(planes.ultimo(usuario.id(), loteId));
	}

	@PutMapping("/seleccion")
	RespuestaSeleccion elegir(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID loteId,
		@Valid @RequestBody SolicitudSeleccion s) {
		return RespuestaSeleccion.de(planes.elegir(usuario.id(), loteId, s.planId(), s.rotacionId()));
	}

	@GetMapping("/seleccion")
	RespuestaSeleccion seleccion(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID loteId) {
		return RespuestaSeleccion.de(planes.seleccion(usuario.id(), loteId));
	}
}
