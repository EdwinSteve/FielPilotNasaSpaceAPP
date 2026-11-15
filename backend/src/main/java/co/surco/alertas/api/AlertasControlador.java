package co.surco.alertas.api;

import co.surco.alertas.aplicacion.ConsultarAlertas;
import co.surco.alertas.dominio.NivelRios;
import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alertas de un punto para la pantalla /alertas del frontend (frontend/src/modulos/alertas/tipos.ts).
 * Protegidas (no están en RutasPublicas). El JSON sale en snake_case: consultado_en.
 */
@RestController
class AlertasControlador {

	private static final String HIDROLOGICA = "Hidrológica";

	private final ConsultarAlertas alertas;

	AlertasControlador(ConsultarAlertas alertas) {
		this.alertas = alertas;
	}

	/** {@code subzona} y {@code tipo} son null cuando el nivel es SIN_ALERTA. */
	record RespuestaRios(String nivel, String subzona, String tipo, String mensaje, String fuente, double lat,
		double lon, OffsetDateTime consultadoEn) {
	}

	record RespuestaSequia(String nivel, String mensaje, String fuente, double lat, double lon,
		OffsetDateTime consultadoEn) {
	}

	@GetMapping("/api/v1/alerts/flood")
	RespuestaRios rios(@RequestParam double lat, @RequestParam double lon) {
		var a = alertas.rios(lat, lon);
		boolean sinAlerta = a.nivel() == NivelRios.SIN_ALERTA;
		return new RespuestaRios(a.nivel().name(), a.subzona(), sinAlerta ? null : HIDROLOGICA, a.nivel().mensaje(),
			"IDEAM", a.lat(), a.lon(), a.consultadoEn());
	}

	/** Indicador propio, no una alerta oficial del IDEAM. */
	@GetMapping("/api/v1/alerts/drought")
	RespuestaSequia sequia(@RequestParam double lat, @RequestParam double lon) {
		var s = alertas.sequia(lat, lon);
		return new RespuestaSequia(s.nivel().name(), s.nivel().mensaje(), "NASA POWER", s.lat(), s.lon(),
			s.consultadoEn());
	}
}
