package co.surco.fincas.api;

import co.surco.fincas.dominio.Coordenada;
import co.surco.fincas.dominio.Terreno;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cuerpos JSON de fincas, en snake_case. Las reglas del polígono (vértices, Colombia, cruces, área)
 * las valida el dominio; aquí solo se exige que los campos lleguen.
 */
final class Dtos {

	private Dtos() {
	}

	record Vertice(@NotNull(message = "Falta la latitud de un punto.") Double lat,
		@NotNull(message = "Falta la longitud de un punto.") Double lon) {

		Coordenada aDominio() {
			return new Coordenada(lat, lon);
		}

		static Vertice de(Coordenada c) {
			return new Vertice(c.lat(), c.lon());
		}
	}

	record SolicitudTerreno(
		@NotBlank(message = "Póngale un nombre a su terreno.") String nombre,
		@NotBlank(message = "Elija el municipio donde queda el terreno.") String municipio,
		@NotNull(message = "Indique si el terreno tiene riego.") Boolean riego,
		@NotNull(message = "Marque el contorno del terreno en el mapa.") List<@Valid @NotNull Vertice> vertices) {
	}

	/** {@code vertices} en orden y sin repetir el primero; {@code poligono_geojson} para mapas y SIG. */
	record RespuestaTerreno(UUID id, String nombre, String municipio, String departamento, boolean riego,
		double areaHa, Vertice centroide, List<Vertice> vertices, String poligonoGeojson, Instant creadoEn) {

		static RespuestaTerreno de(Terreno t) {
			var p = t.poligono();
			return new RespuestaTerreno(t.id(), t.nombre(), t.municipio(), t.departamento(), t.riego(),
				Math.round(p.areaHa() * 10_000) / 10_000.0, Vertice.de(p.centroide()),
				p.vertices().stream().map(Vertice::de).toList(), p.aGeoJson(), t.creadoEn());
		}
	}
}
