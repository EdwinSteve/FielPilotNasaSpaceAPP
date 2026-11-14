package co.surco.identidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.surco.compartido.error.ErrorDominio;
import co.surco.compartido.error.TipoError;
import co.surco.identidad.dominio.SesionRefresco;
import co.surco.identidad.dominio.TokensRefresco;
import java.time.Duration;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CasosDeUsoIdentidadTest {

	Dobles.Reloj reloj;
	Dobles.Usuarios usuarios;
	Dobles.Sesiones sesiones;
	Dobles.Politicas politicas;
	Dobles.Cifrador cifrador;
	RegistrarUsuario registrar;
	IniciarSesion iniciar;
	RefrescarSesion refrescar;
	CerrarSesion cerrar;

	@BeforeEach
	void armar() {
		reloj = new Dobles.Reloj();
		usuarios = new Dobles.Usuarios();
		sesiones = new Dobles.Sesiones();
		politicas = new Dobles.Politicas();
		cifrador = new Dobles.Cifrador();
		AbridorSesiones abridor = new AbridorSesiones(new Dobles.Emisor(), sesiones, Dobles.JWT);
		registrar = new RegistrarUsuario(usuarios, politicas, cifrador, Dobles.municipios("41001", "05001"), abridor, reloj);
		iniciar = new IniciarSesion(usuarios, cifrador, abridor, Dobles.INGRESO, reloj);
		refrescar = new RefrescarSesion(sesiones, usuarios, abridor, reloj);
		cerrar = new CerrarSesion(sesiones, reloj);
	}

	RegistrarUsuario.Comando comando() {
		return new RegistrarUsuario.Comando("Ana Perdomo", "300 123 4567", "", "MiFinca2026", "41001", true, "1.0");
	}

	static void falla(ThrowingCallable accion, String codigo, TipoError tipo) {
		assertThatThrownBy(accion).isInstanceOfSatisfying(ErrorDominio.class, e -> {
			assertThat(e.codigo()).isEqualTo(codigo);
			assertThat(e.tipo()).isEqualTo(tipo);
		});
	}

	@Nested
	class Registro {

		@Test
		void creaLaCuentaGuardaElConsentimientoYAbreSesion() {
			var r = registrar.ejecutar(comando());

			assertThat(r.usuario().celular().valor()).isEqualTo("3001234567");
			assertThat(r.usuario().contrasenaHash()).isEqualTo("hash:MiFinca2026");
			assertThat(r.usuario().correo()).isNull();
			assertThat(politicas.consentimientos).containsExactly(r.usuario().id() + ":1.0");
			assertThat(r.tokens().accessToken()).startsWith("jwt-" + r.usuario().id());
			assertThat(sesiones.porHash(TokensRefresco.hash(r.tokens().refreshToken()))).isPresent();
		}

		@Test
		void exigeAceptarLaPoliticaVigente() {
			var sinConsentimiento = new RegistrarUsuario.Comando("Ana Perdomo", "3001234567", null, "MiFinca2026",
				"41001", false, "1.0");
			falla(() -> registrar.ejecutar(sinConsentimiento), "CONSENTIMIENTO_REQUERIDO", TipoError.SOLICITUD_INVALIDA);

			var versionVieja = new RegistrarUsuario.Comando("Ana Perdomo", "3001234567", null, "MiFinca2026",
				"41001", true, "0.9");
			falla(() -> registrar.ejecutar(versionVieja), "POLITICA_DESACTUALIZADA", TipoError.CONFLICTO);
			assertThat(usuarios.datos).isEmpty();
		}

		@Test
		void rechazaCelularYCorreoDuplicados() {
			registrar.ejecutar(new RegistrarUsuario.Comando("Ana Perdomo", "3001234567", "ana@correo.co",
				"MiFinca2026", "41001", true, "1.0"));

			falla(() -> registrar.ejecutar(new RegistrarUsuario.Comando("Otra", "+57 300 123 4567", null,
				"MiFinca2026", "41001", true, "1.0")), "USUARIO_CELULAR_DUPLICADO", TipoError.CONFLICTO);
			falla(() -> registrar.ejecutar(new RegistrarUsuario.Comando("Otra Persona", "3157654321", "ANA@correo.co",
				"MiFinca2026", "41001", true, "1.0")), "USUARIO_CORREO_DUPLICADO", TipoError.CONFLICTO);
		}

		@Test
		void validaMunicipioYContrasena() {
			falla(() -> registrar.ejecutar(new RegistrarUsuario.Comando("Ana Perdomo", "3001234567", null,
				"MiFinca2026", "99999", true, "1.0")), "IDENTIDAD_MUNICIPIO_INEXISTENTE", TipoError.SOLICITUD_INVALIDA);
			falla(() -> registrar.ejecutar(new RegistrarUsuario.Comando("Ana Perdomo", "3001234567", null,
				"debil", "41001", true, "1.0")), "IDENTIDAD_CONTRASENA_DEBIL", TipoError.SOLICITUD_INVALIDA);
		}
	}

	@Nested
	class Ingreso {

		@BeforeEach
		void cuentaExistente() {
			registrar.ejecutar(comando());
		}

		@Test
		void conCredencialesCorrectasEntregaTokens() {
			Tokens t = iniciar.ejecutar(new IniciarSesion.Comando("+57 300 123 4567", "MiFinca2026"));
			assertThat(t.accessToken()).startsWith("jwt-");
			assertThat(t.expiraEnSegundos()).isEqualTo(900);
		}

		@Test
		void celularInexistenteYContrasenaMalaDanElMismoError() {
			falla(() -> iniciar.ejecutar(new IniciarSesion.Comando("3159999999", "MiFinca2026")),
				"AUTH_CREDENCIALES_INVALIDAS", TipoError.NO_AUTENTICADO);
			falla(() -> iniciar.ejecutar(new IniciarSesion.Comando("3001234567", "Equivocada1")),
				"AUTH_CREDENCIALES_INVALIDAS", TipoError.NO_AUTENTICADO);
			falla(() -> iniciar.ejecutar(new IniciarSesion.Comando("no-es-celular", "x")),
				"AUTH_CREDENCIALES_INVALIDAS", TipoError.NO_AUTENTICADO);
		}

		@Test
		void conCelularInexistenteIgualComparaContraseña() {
			int antes = cifrador.comparaciones;
			falla(() -> iniciar.ejecutar(new IniciarSesion.Comando("3159999999", "x")),
				"AUTH_CREDENCIALES_INVALIDAS", TipoError.NO_AUTENTICADO);
			assertThat(cifrador.comparaciones).isEqualTo(antes + 1);
		}

		@Test
		void bloqueaTrasCincoFallosYDesbloqueaAlPasarElTiempo() {
			var mala = new IniciarSesion.Comando("3001234567", "Equivocada1");
			for (int i = 0; i < 4; i++) {
				falla(() -> iniciar.ejecutar(mala), "AUTH_CREDENCIALES_INVALIDAS", TipoError.NO_AUTENTICADO);
			}
			falla(() -> iniciar.ejecutar(mala), "AUTH_CUENTA_BLOQUEADA", TipoError.BLOQUEADO);
			// Bloqueada: ni la contraseña correcta entra
			falla(() -> iniciar.ejecutar(new IniciarSesion.Comando("3001234567", "MiFinca2026")),
				"AUTH_CUENTA_BLOQUEADA", TipoError.BLOQUEADO);

			reloj.avanzar(Duration.ofMinutes(15));
			assertThat(iniciar.ejecutar(new IniciarSesion.Comando("3001234567", "MiFinca2026"))).isNotNull();
		}
	}

	@Nested
	class Refresco {

		Tokens tokens;

		@BeforeEach
		void sesionAbierta() {
			tokens = registrar.ejecutar(comando()).tokens();
		}

		@Test
		void rotaElRefreshTokenEnLaMismaFamilia() {
			SesionRefresco original = sesiones.porHash(TokensRefresco.hash(tokens.refreshToken())).orElseThrow();
			reloj.avanzar(Duration.ofMinutes(20));

			Tokens nuevos = refrescar.ejecutar(tokens.refreshToken());

			assertThat(nuevos.refreshToken()).isNotEqualTo(tokens.refreshToken());
			assertThat(nuevos.accessToken()).isNotEqualTo(tokens.accessToken());
			SesionRefresco nueva = sesiones.porHash(TokensRefresco.hash(nuevos.refreshToken())).orElseThrow();
			assertThat(nueva.familia()).isEqualTo(original.familia());
			assertThat(sesiones.porHash(original.hashToken()).orElseThrow().usadaEn()).isNotNull();
		}

		@Test
		void reutilizarUnRefreshTokenRevocaTodaLaFamilia() {
			Tokens nuevos = refrescar.ejecutar(tokens.refreshToken());
			SesionRefresco sesion = sesiones.porHash(TokensRefresco.hash(nuevos.refreshToken())).orElseThrow();

			// Alguien reutiliza el token viejo (robado): se revoca todo, también el token nuevo del dueño
			falla(() -> refrescar.ejecutar(tokens.refreshToken()), "AUTH_REFRESH_INVALIDO", TipoError.NO_AUTENTICADO);
			assertThat(sesiones.vigentesDeFamilia(sesion.familia())).isZero();
			falla(() -> refrescar.ejecutar(nuevos.refreshToken()), "AUTH_REFRESH_INVALIDO", TipoError.NO_AUTENTICADO);
		}

		@Test
		void rechazaTokensVencidosODesconocidos() {
			reloj.avanzar(Duration.ofDays(30));
			falla(() -> refrescar.ejecutar(tokens.refreshToken()), "AUTH_REFRESH_INVALIDO", TipoError.NO_AUTENTICADO);
			falla(() -> refrescar.ejecutar("inventado"), "AUTH_REFRESH_INVALIDO", TipoError.NO_AUTENTICADO);
			falla(() -> refrescar.ejecutar(""), "AUTH_REFRESH_INVALIDO", TipoError.NO_AUTENTICADO);
		}

		@Test
		void cerrarSesionInvalidaElRefreshTokenYEsIdempotente() {
			cerrar.ejecutar(tokens.refreshToken());
			cerrar.ejecutar(tokens.refreshToken());
			cerrar.ejecutar("desconocido");
			falla(() -> refrescar.ejecutar(tokens.refreshToken()), "AUTH_REFRESH_INVALIDO", TipoError.NO_AUTENTICADO);
		}
	}
}
