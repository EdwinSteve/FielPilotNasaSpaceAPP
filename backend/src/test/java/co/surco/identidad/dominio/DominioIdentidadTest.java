package co.surco.identidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.surco.compartido.error.ErrorDominio;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DominioIdentidadTest {

	static final Instant AHORA = Instant.parse("2026-11-14T15:00:00Z");

	@Nested
	class CelularColombiano {

		@ParameterizedTest
		@ValueSource(strings = {"3001234567", "300 123 4567", "+57 300-123-4567", "573001234567", "(300) 123.4567"})
		void normalizaADiezDigitos(String texto) {
			assertThat(Celular.de(texto).valor()).isEqualTo("3001234567");
			assertThat(Celular.de(texto).internacional()).isEqualTo("+573001234567");
		}

		@ParameterizedTest
		@ValueSource(strings = {"", "300123456", "30012345678", "6011234567", "abc1234567", "+1 300 123 4567"})
		void rechazaLoQueNoEsUnMovilColombiano(String texto) {
			assertThatThrownBy(() -> Celular.de(texto))
				.isInstanceOf(ErrorDominio.class)
				.extracting(e -> ((ErrorDominio) e).codigo()).isEqualTo("IDENTIDAD_CELULAR_INVALIDO");
		}
	}

	@Nested
	class CorreoOpcional {

		@Test
		void vacioEsSinCorreo() {
			assertThat(Correo.deOpcional("  ")).isNull();
			assertThat(Correo.deOpcional(null)).isNull();
		}

		@Test
		void seGuardaEnMinusculas() {
			assertThat(Correo.deOpcional(" Ana.Perdomo@Correo.CO ").valor()).isEqualTo("ana.perdomo@correo.co");
		}

		@Test
		void rechazaFormatoInvalido() {
			assertThatThrownBy(() -> Correo.deOpcional("no-es-correo")).isInstanceOf(ErrorDominio.class);
		}
	}

	@Nested
	class Contrasenas {

		@Test
		void exigeOchoCaracteresConLetraYNumero() {
			PoliticaContrasenas.validar("MiFinca2026");
			PoliticaContrasenas.validar("ñandú2026");
			for (String debil : new String[] {"corta1", "solamenteletras", "1234567890", null}) {
				assertThatThrownBy(() -> PoliticaContrasenas.validar(debil)).isInstanceOf(ErrorDominio.class);
			}
		}
	}

	@Nested
	class BloqueoPorIntentos {

		Usuario usuario() {
			return Usuario.nuevo("Ana Perdomo", Celular.de("3001234567"), null, "hash", "41001", AHORA);
		}

		@Test
		void seBloqueaAlLlegarAlMaximoYReiniciaElConteo() {
			Usuario u = usuario();
			for (int i = 0; i < 4; i++) {
				u.registrarIntentoFallido(AHORA, 5, Duration.ofMinutes(15));
			}
			assertThat(u.estaBloqueado(AHORA)).isFalse();

			u.registrarIntentoFallido(AHORA, 5, Duration.ofMinutes(15));
			assertThat(u.estaBloqueado(AHORA)).isTrue();
			assertThat(u.estaBloqueado(AHORA.plus(Duration.ofMinutes(15)))).isFalse();
			assertThat(u.intentosFallidos()).isZero();
		}

		@Test
		void unIngresoExitosoLimpiaLosIntentos() {
			Usuario u = usuario();
			u.registrarIntentoFallido(AHORA, 5, Duration.ofMinutes(15));
			u.registrarIngresoExitoso();
			assertThat(u.intentosFallidos()).isZero();
		}

		@Test
		void elNombreSeLimpiaYSeValida() {
			Usuario u = Usuario.nuevo("  Ana   Perdomo ", Celular.de("3001234567"), null, "h", "41001", AHORA);
			assertThat(u.nombreCompleto()).isEqualTo("Ana Perdomo");
			assertThat(u.rol()).isEqualTo(Rol.PRODUCTOR);
			assertThatThrownBy(() -> Usuario.nuevo(" A ", Celular.de("3001234567"), null, "h", "41001", AHORA))
				.isInstanceOf(ErrorDominio.class);
		}
	}

	@Nested
	class RefreshTokens {

		@Test
		void sonAleatoriosYSuHashEsEstable() {
			String a = TokensRefresco.generar();
			String b = TokensRefresco.generar();
			assertThat(a).hasSize(43).isNotEqualTo(b);
			assertThat(TokensRefresco.hash(a)).hasSize(64).isEqualTo(TokensRefresco.hash(a)).isNotEqualTo(TokensRefresco.hash(b));
		}
	}
}
