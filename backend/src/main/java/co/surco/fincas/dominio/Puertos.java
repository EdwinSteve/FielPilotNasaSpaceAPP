package co.surco.fincas.dominio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puertos que el dominio de fincas necesita del exterior. La infraestructura los implementa
 * (PostgreSQL, módulo territorio) y las pruebas los reemplazan por versiones en memoria.
 */
public final class Puertos {

	private Puertos() {
	}

	/** Todas las operaciones filtran por el dueño: un terreno ajeno es igual a uno inexistente. */
	public interface RepositorioTerrenos {
		void guardar(Terreno terreno);

		List<Terreno> deUsuario(UUID usuarioId);

		Optional<Terreno> porId(UUID usuarioId, UUID terrenoId);

		long contarDeUsuario(UUID usuarioId);

		/** @return false si no existía o no era del usuario */
		boolean eliminar(UUID usuarioId, UUID terrenoId);
	}

	/** ¿Existe el municipio? Lo responde el módulo territorio. */
	public interface ValidadorMunicipios {
		boolean existe(String codigoMunicipio);
	}
}
