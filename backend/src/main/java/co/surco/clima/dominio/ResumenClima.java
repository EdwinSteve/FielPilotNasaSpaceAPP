package co.surco.clima.dominio;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Lo que responde {@code GET /api/v1/clima/resumen}. Son números sin formato: el frontend les pone
 * unidades y textos. Un campo en {@code null} significa "sin dato reciente", nunca cero.
 *
 * @param satelite {@code null} si NASA POWER no respondió
 * @param fuentes  nombres de las fuentes que sí aportaron datos a esta respuesta
 */
public record ResumenClima(Ubicacion ubicacion, OffsetDateTime actualizado, Actual actual, Satelite satelite,
	List<Dia> pronostico, List<Aviso> avisos, List<String> fuentes) {

	/** @param municipio nombre del municipio, o {@code null} si solo se pidió por coordenadas */
	public record Ubicacion(String municipio, String departamento, double lat, double lon, Double altitudM) {
	}

	/**
	 * @param icono          "sun", "cloudsun" o "rain"
	 * @param vientoRumbo    de dónde viene el viento: N, NE, E, SE, S, SO, O o NO
	 * @param lluviaDiaMm    lluvia de las últimas 24 horas
	 * @param lluviaSemanaMm lluvia de los últimos 7 días
	 */
	public record Actual(double temperatura, Double sensacion, String descripcion, String icono, Integer humedad,
		Double vientoKmh, String vientoRumbo, Double rafagasKmh, double lluviaDiaMm, double lluviaSemanaMm) {
	}

	/**
	 * Datos diarios de NASA POWER: llegan con unos días de retraso, por eso cada uno trae su fecha.
	 *
	 * @param radiacionKwh    radiación solar del día, en kWh/m²
	 * @param humedadSueloPct humedad del suelo en la zona de raíces, de 0 a 100
	 */
	public record Satelite(Double radiacionKwh, LocalDate radiacionFecha, Double humedadSueloPct,
		LocalDate humedadSueloFecha) {
	}

	/** Un día del pronóstico; el primero es hoy. */
	public record Dia(LocalDate fecha, String descripcion, String icono, double tempMax, double tempMin,
		double lluviaMm, Integer probLluvia, Double indiceUv, boolean lluviaFuerte) {
	}

	/**
	 * @param tipo    RIOS (alerta hidrológica del IDEAM), LLUVIA_FUERTE o SEQUIA
	 * @param zona    a qué zona aplica, o {@code null}
	 * @param oficial true solo para las alertas que emite el IDEAM; las demás las calcula SURCO
	 */
	public record Aviso(String tipo, Nivel nivel, String titulo, String mensaje, String zona, String fuente,
		boolean oficial) {
	}
}
