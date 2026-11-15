package co.surco.clima.api;

import co.surco.clima.aplicacion.ConsultarClima;
import co.surco.clima.dominio.ErroresClima.ClimaNoDisponible;
import co.surco.clima.dominio.ResumenClima;
import co.surco.compartido.error.RespuestaError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Clima para la pantalla de inicio. Exige sesión (no está en RutasPublicas).
 *
 * <pre>
 * GET /api/v1/clima/resumen?municipio=41001                       cabecera del municipio
 * GET /api/v1/clima/resumen?municipio=41001&amp;lat=2.93&amp;lon=-75.28   punto del terreno
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/clima")
class ClimaControlador {

	private final ConsultarClima consultarClima;

	ClimaControlador(ConsultarClima consultarClima) {
		this.consultarClima = consultarClima;
	}

	@GetMapping("/resumen")
	ResumenClima resumen(@RequestParam(required = false) String municipio, @RequestParam(required = false) Double lat,
		@RequestParam(required = false) Double lon) {
		return consultarClima.resumen(municipio, lat, lon);
	}

	/** La fuente del pronóstico no respondió: 503, para que la app ofrezca reintentar. */
	@ExceptionHandler(ClimaNoDisponible.class)
	ResponseEntity<RespuestaError> noDisponible(ClimaNoDisponible e) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
			.body(RespuestaError.de("CLIMA_NO_DISPONIBLE", e.getMessage()));
	}
}
