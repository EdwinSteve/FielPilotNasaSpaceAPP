package co.surco.fincas.aplicacion;

import co.surco.fincas.ConsultaTerrenos;
import co.surco.fincas.UbicacionTerreno;
import co.surco.fincas.dominio.ErroresFincas;
import co.surco.fincas.dominio.Puertos.RepositorioTerrenos;
import co.surco.fincas.dominio.Terreno;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lecturas de terrenos, siempre del productor autenticado. Implementa también la API pública del módulo. */
@Service
@Transactional(readOnly = true)
public class ConsultasTerrenos implements ConsultaTerrenos {

	private final RepositorioTerrenos terrenos;

	public ConsultasTerrenos(RepositorioTerrenos terrenos) {
		this.terrenos = terrenos;
	}

	public List<Terreno> de(UUID usuarioId) {
		return terrenos.deUsuario(usuarioId);
	}

	public Terreno obtener(UUID usuarioId, UUID terrenoId) {
		return terrenos.porId(usuarioId, terrenoId).orElseThrow(ErroresFincas::terrenoNoEncontrado);
	}

	@Override
	public List<UbicacionTerreno> terrenosDe(UUID usuarioId) {
		return de(usuarioId).stream().map(ConsultasTerrenos::ubicacion).toList();
	}

	@Override
	public Optional<UbicacionTerreno> terreno(UUID usuarioId, UUID terrenoId) {
		return terrenos.porId(usuarioId, terrenoId).map(ConsultasTerrenos::ubicacion);
	}

	static UbicacionTerreno ubicacion(Terreno t) {
		var c = t.poligono().centroide();
		return new UbicacionTerreno(t.id(), t.usuarioId(), t.nombre(), t.municipio(), t.riego(), c.lat(), c.lon(),
			t.poligono().areaHa(), t.poligono().aGeoJson());
	}
}
