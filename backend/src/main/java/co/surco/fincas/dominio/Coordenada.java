package co.surco.fincas.dominio;

/**
 * Un punto sobre la Tierra en grados decimales (WGS 84, el de GPS y Leaflet).
 *
 * @param lat latitud, positiva al norte
 * @param lon longitud, negativa al occidente (Colombia está entre -82 y -66,8)
 */
public record Coordenada(double lat, double lon) {

	public Coordenada {
		if (!Double.isFinite(lat) || !Double.isFinite(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
			throw ErroresFincas.coordenadaInvalida();
		}
	}

	/** Dentro del recuadro de Colombia continental e insular (San Andrés incluido). */
	public boolean enColombia() {
		return lat >= -4.3 && lat <= 13.6 && lon >= -82.0 && lon <= -66.8;
	}
}
