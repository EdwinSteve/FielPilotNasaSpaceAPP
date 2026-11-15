package co.surco.fincas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.surco.compartido.error.ErrorDominio;
import co.surco.fincas.dominio.Coordenada;
import co.surco.fincas.dominio.ErroresFincas;
import co.surco.fincas.dominio.Puertos.RepositorioTerrenos;
import co.surco.fincas.dominio.Terreno;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Casos de uso de fincas con un repositorio en memoria: sin base de datos ni Spring. */
class CasosDeUsoFincasTest {

	static class TerrenosEnMemoria implements RepositorioTerrenos {
		final List<Terreno> filas = new ArrayList<>();

		@Override
		public void guardar(Terreno terreno) {
			filas.add(terreno);
		}

		@Override
		public List<Terreno> deUsuario(UUID usuarioId) {
			return filas.stream().filter(t -> t.usuarioId().equals(usuarioId)).toList();
		}

		@Override
		public Optional<Terreno> porId(UUID usuarioId, UUID terrenoId) {
			return deUsuario(usuarioId).stream().filter(t -> t.id().equals(terrenoId)).findFirst();
		}

		@Override
		public long contarDeUsuario(UUID usuarioId) {
			return deUsuario(usuarioId).size();
		}

		@Override
		public boolean eliminar(UUID usuarioId, UUID terrenoId) {
			return filas.removeIf(t -> t.usuarioId().equals(usuarioId) && t.id().equals(terrenoId));
		}
	}

	static final List<Coordenada> CUADRADO = List.of(new Coordenada(5.0, -75.5), new Coordenada(5.0, -75.498),
		new Coordenada(5.002, -75.498), new Coordenada(5.002, -75.5));
	static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-15T13:00:00Z"), ZoneOffset.UTC);

	TerrenosEnMemoria repo;
	RegistrarTerreno registrar;
	ConsultasTerrenos consultas;
	EliminarTerreno eliminar;
	final UUID ana = UUID.randomUUID();
	final UUID beto = UUID.randomUUID();

	@BeforeEach
	void armar() {
		repo = new TerrenosEnMemoria();
		registrar = new RegistrarTerreno(repo, codigo -> codigo.equals("17001") || codigo.equals("05001"), RELOJ);
		consultas = new ConsultasTerrenos(repo);
		eliminar = new EliminarTerreno(repo);
	}

	Terreno registrarDe(UUID usuario) {
		return registrar.ejecutar(usuario, new RegistrarTerreno.Solicitud("Lote 1", "17001", true, CUADRADO));
	}

	@Test
	void registraYConsultaSoloLosPropios() {
		Terreno t = registrarDe(ana);
		registrarDe(beto);
		assertThat(t.creadoEn()).isEqualTo(RELOJ.instant());
		assertThat(consultas.de(ana)).extracting(Terreno::id).containsExactly(t.id());
		assertThat(consultas.obtener(ana, t.id()).nombre()).isEqualTo("Lote 1");
		assertThatThrownBy(() -> consultas.obtener(beto, t.id()))
			.isInstanceOf(ErrorDominio.class).hasMessage("Ese terreno no existe.");
	}

	@Test
	void apiPublicaEntregaElCentroide() {
		Terreno t = registrarDe(ana);
		var u = consultas.terreno(ana, t.id()).orElseThrow();
		assertThat(u.latitud()).isBetween(5.0, 5.002);
		assertThat(u.longitud()).isBetween(-75.5, -75.498);
		assertThat(u.areaHa()).isPositive();
		assertThat(consultas.terreno(beto, t.id())).isEmpty();
	}

	@Test
	void rechazaMunicipioInexistente() {
		var s = new RegistrarTerreno.Solicitud("Lote", "99999", false, CUADRADO);
		assertThatThrownBy(() -> registrar.ejecutar(ana, s))
			.isInstanceOfSatisfying(ErrorDominio.class, e -> assertThat(e.codigo()).isEqualTo("FINCAS_MUNICIPIO_INEXISTENTE"));
		assertThat(repo.filas).isEmpty();
	}

	@Test
	void limiteDeTerrenosPorCuenta() {
		for (int i = 0; i < ErroresFincas.MAXIMO_TERRENOS; i++) {
			registrarDe(ana);
		}
		assertThatThrownBy(() -> registrarDe(ana))
			.isInstanceOfSatisfying(ErrorDominio.class, e -> assertThat(e.codigo()).isEqualTo("FINCAS_LIMITE_TERRENOS"));
		registrarDe(beto); // el límite es por cuenta
	}

	@Test
	void soloElDuenoBorra() {
		Terreno t = registrarDe(ana);
		assertThatThrownBy(() -> eliminar.ejecutar(beto, t.id())).isInstanceOf(ErrorDominio.class);
		eliminar.ejecutar(ana, t.id());
		assertThat(consultas.de(ana)).isEmpty();
	}
}
