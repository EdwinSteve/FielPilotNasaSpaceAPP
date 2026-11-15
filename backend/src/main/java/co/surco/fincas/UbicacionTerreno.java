package co.surco.fincas;

import java.util.UUID;

/**
 * Lo que otros módulos necesitan saber de un terreno.
 *
 * @param municipio       código DIVIPOLA de 5 dígitos
 * @param latitud         latitud del centroide del polígono
 * @param longitud        longitud del centroide del polígono
 * @param poligonoGeoJson contorno en GeoJSON (RFC 7946, [lon, lat])
 */
public record UbicacionTerreno(UUID id, UUID usuarioId, String nombre, String municipio, boolean riego,
	double latitud, double longitud, double areaHa, String poligonoGeoJson) {
}
