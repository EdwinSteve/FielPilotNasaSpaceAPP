package co.surco.fincas.infraestructura.persistencia;

import co.surco.fincas.dominio.Puertos.RepositorioTerrenos;
import co.surco.fincas.dominio.Terreno;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Adaptadores PostgreSQL de los puertos de fincas. */
final class AdaptadoresPostgres {

	private AdaptadoresPostgres() {
	}

	@Repository
	static class Terrenos implements RepositorioTerrenos {

		private final TerrenosJpa jpa;

		Terrenos(TerrenosJpa jpa) {
			this.jpa = jpa;
		}

		@Override
		public void guardar(Terreno terreno) {
			jpa.saveAndFlush(TerrenoJpa.desde(terreno));
		}

		@Override
		public List<Terreno> deUsuario(UUID usuarioId) {
			return jpa.findByUsuarioIdOrderByCreadoEnAsc(usuarioId).stream().map(TerrenoJpa::aDominio).toList();
		}

		@Override
		public Optional<Terreno> porId(UUID usuarioId, UUID terrenoId) {
			return jpa.findByIdAndUsuarioId(terrenoId, usuarioId).map(TerrenoJpa::aDominio);
		}

		@Override
		public long contarDeUsuario(UUID usuarioId) {
			return jpa.countByUsuarioId(usuarioId);
		}

		@Override
		public boolean eliminar(UUID usuarioId, UUID terrenoId) {
			return jpa.borrar(terrenoId, usuarioId) > 0;
		}
	}
}
