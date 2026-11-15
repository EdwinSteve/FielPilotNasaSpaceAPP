package co.surco.recomendacion.infraestructura.fincas;

import co.surco.fincas.ConsultaTerrenos;
import co.surco.recomendacion.dominio.Puertos.TerrenoUbicado;
import co.surco.recomendacion.dominio.Puertos.TerrenosDelProductor;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Pregunta al módulo fincas (por su API pública) dónde queda el terreno del productor. */
@Component
class TerrenosFincas implements TerrenosDelProductor {

	private final ConsultaTerrenos fincas;

	TerrenosFincas(ConsultaTerrenos fincas) {
		this.fincas = fincas;
	}

	@Override
	public Optional<TerrenoUbicado> de(UUID usuarioId, UUID terrenoId) {
		return fincas.terreno(usuarioId, terrenoId).map(t -> new TerrenoUbicado(t.id(), t.nombre(), t.municipio(),
			t.latitud(), t.longitud(), t.areaHa(), t.riego()));
	}
}
