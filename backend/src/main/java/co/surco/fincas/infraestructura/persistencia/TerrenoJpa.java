package co.surco.fincas.infraestructura.persistencia;

import co.surco.fincas.dominio.Poligono;
import co.surco.fincas.dominio.Terreno;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Tabla fincas.terrenos (migración V4). Solo la infraestructura conoce esta clase. */
@Entity
@Table(schema = "fincas", name = "terrenos")
class TerrenoJpa {

	@Id
	private UUID id;
	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;
	@Column(nullable = false)
	private String nombre;
	@Column(nullable = false)
	private String municipio;
	@Column(nullable = false)
	private boolean riego;
	@Column(name = "poligono_geojson", nullable = false)
	private String poligonoGeoJson;
	@Column(name = "numero_vertices", nullable = false)
	private int numeroVertices;
	@Column(name = "area_ha", nullable = false)
	private double areaHa;
	@Column(name = "centroide_lat", nullable = false)
	private double centroideLat;
	@Column(name = "centroide_lon", nullable = false)
	private double centroideLon;
	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	protected TerrenoJpa() {
	}

	static TerrenoJpa desde(Terreno t) {
		TerrenoJpa e = new TerrenoJpa();
		e.id = t.id();
		e.usuarioId = t.usuarioId();
		e.nombre = t.nombre();
		e.municipio = t.municipio();
		e.riego = t.riego();
		e.poligonoGeoJson = t.poligono().aGeoJson();
		e.numeroVertices = t.poligono().vertices().size();
		e.areaHa = t.poligono().areaHa();
		e.centroideLat = t.poligono().centroide().lat();
		e.centroideLon = t.poligono().centroide().lon();
		e.creadoEn = t.creadoEn();
		return e;
	}

	Terreno aDominio() {
		return new Terreno(id, usuarioId, nombre, municipio, riego, Poligono.desdeGeoJson(poligonoGeoJson), creadoEn);
	}
}
