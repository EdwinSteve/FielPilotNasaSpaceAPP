package co.surco.fincas.aplicacion;

import co.surco.fincas.dominio.ErroresFincas;
import co.surco.fincas.dominio.Puertos.RepositorioTerrenos;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Borra un terreno del productor. Uno ajeno responde igual que uno inexistente (404). */
@Service
@Transactional
public class EliminarTerreno {

	private final RepositorioTerrenos terrenos;

	public EliminarTerreno(RepositorioTerrenos terrenos) {
		this.terrenos = terrenos;
	}

	public void ejecutar(UUID usuarioId, UUID terrenoId) {
		if (!terrenos.eliminar(usuarioId, terrenoId)) {
			throw ErroresFincas.terrenoNoEncontrado();
		}
	}
}
