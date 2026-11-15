package co.surco.clima.api;

import co.surco.clima.aplicacion.ConsultarClimaActual;
import co.surco.clima.aplicacion.ConsultarClimaActual.ClimaActual;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Condiciones actuales del punto para las tarjetas del tablero. Protegida (no está en RutasPublicas). */
@RestController
class ClimaControlador {

	private final ConsultarClimaActual clima;

	ClimaControlador(ConsultarClimaActual clima) {
		this.clima = clima;
	}

	/** {@code {lat, lon, consultado, condiciones: [{variable, valor, unidad, detalle, fuente, medido}]}}. */
	@GetMapping("/api/v1/weather/current")
	ClimaActual actual(@RequestParam double lat, @RequestParam double lon) {
		return clima.en(lat, lon);
	}
}
