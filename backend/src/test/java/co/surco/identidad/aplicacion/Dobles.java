package co.surco.identidad.aplicacion;

import co.surco.compartido.seguridad.PropiedadesJwt;
import co.surco.identidad.dominio.Celular;
import co.surco.identidad.dominio.Correo;
import co.surco.identidad.dominio.PoliticaDatos;
import co.surco.identidad.dominio.Puertos;
import co.surco.identidad.dominio.SesionRefresco;
import co.surco.identidad.dominio.Usuario;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Implementaciones en memoria de los puertos: prueban los casos de uso sin base de datos ni Spring. */
final class Dobles {

	private Dobles() {
	}

	static final PropiedadesJwt JWT = new PropiedadesJwt("x".repeat(64), "surco-api", "surco-app",
		Duration.ofMinutes(15), Duration.ofDays(30));
	static final PropiedadesIngreso INGRESO = new PropiedadesIngreso(5, Duration.ofMinutes(15));

	/** Reloj que se puede adelantar. */
	static class Reloj extends Clock {
		Instant ahora = Instant.parse("2026-11-14T15:00:00Z");

		void avanzar(Duration d) {
			ahora = ahora.plus(d);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return ahora;
		}
	}

	static class Usuarios implements Puertos.RepositorioUsuarios {
		final Map<UUID, Usuario> datos = new HashMap<>();

		@Override
		public Optional<Usuario> porId(UUID id) {
			return Optional.ofNullable(datos.get(id));
		}

		@Override
		public Optional<Usuario> porCelular(Celular celular) {
			return datos.values().stream().filter(u -> u.celular().equals(celular)).findFirst();
		}

		@Override
		public boolean existeCelular(Celular celular) {
			return porCelular(celular).isPresent();
		}

		@Override
		public boolean existeCorreo(Correo correo) {
			return datos.values().stream().anyMatch(u -> Objects.equals(u.correo(), correo));
		}

		@Override
		public void guardar(Usuario usuario) {
			datos.put(usuario.id(), usuario);
		}
	}

	static class Sesiones implements Puertos.RepositorioSesiones {
		final Map<String, SesionRefresco> porHash = new HashMap<>();

		@Override
		public Optional<SesionRefresco> porHash(String hashToken) {
			return Optional.ofNullable(porHash.get(hashToken));
		}

		@Override
		public void guardar(SesionRefresco sesion) {
			porHash.put(sesion.hashToken(), sesion);
		}

		@Override
		public void revocarFamilia(UUID familia, Instant ahora) {
			revocarSi(s -> s.familia().equals(familia), ahora);
		}

		@Override
		public void revocarTodasDelUsuario(UUID usuarioId, Instant ahora) {
			revocarSi(s -> s.usuarioId().equals(usuarioId), ahora);
		}

		private void revocarSi(java.util.function.Predicate<SesionRefresco> condicion, Instant ahora) {
			porHash.replaceAll((h, s) -> condicion.test(s) && s.revocadaEn() == null
				? new SesionRefresco(s.id(), s.usuarioId(), s.familia(), s.hashToken(), s.creadaEn(), s.expiraEn(),
					s.usadaEn(), ahora)
				: s);
		}

		long vigentesDeFamilia(UUID familia) {
			return porHash.values().stream().filter(s -> s.familia().equals(familia) && s.revocadaEn() == null).count();
		}
	}

	static class Politicas implements Puertos.RepositorioPoliticas {
		PoliticaDatos vigente = new PoliticaDatos("1.0", "/politica-de-datos", "texto");
		final List<String> consentimientos = new ArrayList<>();

		@Override
		public Optional<PoliticaDatos> vigente() {
			return Optional.ofNullable(vigente);
		}

		@Override
		public void registrarConsentimiento(UUID usuarioId, String versionPolitica, Instant ahora) {
			consentimientos.add(usuarioId + ":" + versionPolitica);
		}
	}

	/** "Hash" legible para las pruebas. Cuenta las comparaciones para verificar el hash señuelo. */
	static class Cifrador implements Puertos.CifradorContrasenas {
		int comparaciones;

		@Override
		public String cifrar(String contrasena) {
			return "hash:" + contrasena;
		}

		@Override
		public boolean coincide(String contrasena, String hash) {
			comparaciones++;
			return hash.equals("hash:" + contrasena);
		}
	}

	static class Emisor implements Puertos.EmisorTokensAcceso {
		@Override
		public String emitir(Usuario usuario, Instant ahora) {
			return "jwt-" + usuario.id() + "-" + ahora.getEpochSecond();
		}

		@Override
		public long duracionSegundos() {
			return 900;
		}
	}

	static Puertos.ValidadorMunicipios municipios(String... existentes) {
		Set<String> codigos = Set.of(existentes);
		return codigos::contains;
	}
}
