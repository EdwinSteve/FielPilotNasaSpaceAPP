package co.surco.identidad.infraestructura.persistencia;

import co.surco.identidad.dominio.Celular;
import co.surco.identidad.dominio.Correo;
import co.surco.identidad.dominio.ErroresIdentidad;
import co.surco.identidad.dominio.PoliticaDatos;
import co.surco.identidad.dominio.Puertos.RepositorioPoliticas;
import co.surco.identidad.dominio.Puertos.RepositorioSesiones;
import co.surco.identidad.dominio.Puertos.RepositorioUsuarios;
import co.surco.identidad.dominio.SesionRefresco;
import co.surco.identidad.dominio.Usuario;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

/** Adaptadores PostgreSQL de los puertos de identidad. */
final class AdaptadoresPostgres {

	private AdaptadoresPostgres() {
	}

	@Repository
	static class Usuarios implements RepositorioUsuarios {

		private final UsuariosJpa jpa;

		Usuarios(UsuariosJpa jpa) {
			this.jpa = jpa;
		}

		@Override
		public Optional<Usuario> porId(UUID id) {
			return jpa.findById(id).map(UsuarioJpa::aDominio);
		}

		@Override
		public Optional<Usuario> porCelular(Celular celular) {
			return jpa.findByCelular(celular.valor()).map(UsuarioJpa::aDominio);
		}

		@Override
		public boolean existeCelular(Celular celular) {
			return jpa.existsByCelular(celular.valor());
		}

		@Override
		public boolean existeCorreo(Correo correo) {
			return jpa.existsByCorreo(correo.valor());
		}

		/**
		 * saveAndFlush para que una violación de unicidad salga aquí y no al confirmar la transacción:
		 * si dos registros con el mismo celular llegan a la vez, el segundo recibe 409 y no 500.
		 */
		@Override
		public void guardar(Usuario usuario) {
			try {
				jpa.saveAndFlush(UsuarioJpa.desde(usuario));
			} catch (DataIntegrityViolationException e) {
				String detalle = String.valueOf(e.getMostSpecificCause().getMessage());
				if (detalle.contains("usuarios_correo_key")) {
					throw ErroresIdentidad.correoDuplicado();
				}
				if (detalle.contains("usuarios_celular_key")) {
					throw ErroresIdentidad.celularDuplicado();
				}
				throw e;
			}
		}
	}

	@Repository
	static class Sesiones implements RepositorioSesiones {

		private final SesionesJpa jpa;

		Sesiones(SesionesJpa jpa) {
			this.jpa = jpa;
		}

		@Override
		public Optional<SesionRefresco> porHash(String hashToken) {
			return jpa.findByHashToken(hashToken).map(SesionJpa::aDominio);
		}

		@Override
		public void guardar(SesionRefresco sesion) {
			jpa.save(SesionJpa.desde(sesion));
		}

		@Override
		public void revocarFamilia(UUID familia, Instant ahora) {
			jpa.revocarFamilia(familia, ahora);
		}

		@Override
		public void revocarTodasDelUsuario(UUID usuarioId, Instant ahora) {
			jpa.revocarDeUsuario(usuarioId, ahora);
		}
	}

	@Repository
	static class Politicas implements RepositorioPoliticas {

		private final PoliticasJpa politicas;
		private final ConsentimientosJpa consentimientos;

		Politicas(PoliticasJpa politicas, ConsentimientosJpa consentimientos) {
			this.politicas = politicas;
			this.consentimientos = consentimientos;
		}

		@Override
		public Optional<PoliticaDatos> vigente() {
			return politicas.findByVigenteTrue().map(PoliticaJpa::aDominio);
		}

		@Override
		public void registrarConsentimiento(UUID usuarioId, String versionPolitica, Instant ahora) {
			consentimientos.save(new ConsentimientoJpa(usuarioId, versionPolitica, ahora));
		}
	}
}
