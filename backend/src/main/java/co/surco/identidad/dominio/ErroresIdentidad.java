package co.surco.identidad.dominio;

import co.surco.compartido.error.ErrorDominio;
import co.surco.compartido.error.TipoError;

/**
 * Errores de negocio de identidad. Los códigos son el contrato con el frontend
 * ({@code frontend/src/modulos/identidad/tipos.ts}); no los cambie sin cambiarlos allá.
 * Los nombres de campo son los del JSON del formulario.
 */
public final class ErroresIdentidad {

	private ErroresIdentidad() {
	}

	public static ErrorDominio celularInvalido() {
		return ErrorDominio.deCampo("IDENTIDAD_CELULAR_INVALIDO", "celular",
			"El celular debe tener 10 dígitos y empezar por el número 3.");
	}

	public static ErrorDominio correoInvalido() {
		return ErrorDominio.deCampo("IDENTIDAD_CORREO_INVALIDO", "correo",
			"Escriba un correo electrónico válido o deje este campo vacío.");
	}

	public static ErrorDominio nombreInvalido() {
		return ErrorDominio.deCampo("IDENTIDAD_NOMBRE_INVALIDO", "nombre_completo",
			"Escriba su nombre y apellido completos (mínimo 3 letras).");
	}

	public static ErrorDominio contrasenaDebil() {
		return ErrorDominio.deCampo("IDENTIDAD_CONTRASENA_DEBIL", "contrasena",
			"La contraseña debe tener mínimo 8 caracteres, al menos una letra y un número.");
	}

	public static ErrorDominio municipioInexistente() {
		return ErrorDominio.deCampo("IDENTIDAD_MUNICIPIO_INEXISTENTE", "municipio_residencia",
			"Elija un municipio de la lista.");
	}

	public static ErrorDominio celularDuplicado() {
		return new ErrorDominio("USUARIO_CELULAR_DUPLICADO", "Ese celular ya está registrado.", TipoError.CONFLICTO);
	}

	public static ErrorDominio correoDuplicado() {
		return new ErrorDominio("USUARIO_CORREO_DUPLICADO", "Ese correo ya está registrado.", TipoError.CONFLICTO);
	}

	public static ErrorDominio consentimientoRequerido() {
		return new ErrorDominio("CONSENTIMIENTO_REQUERIDO",
			"Debe autorizar el tratamiento de sus datos para crear la cuenta.", TipoError.SOLICITUD_INVALIDA);
	}

	public static ErrorDominio politicaDesactualizada() {
		return new ErrorDominio("POLITICA_DESACTUALIZADA",
			"La política de datos cambió. Léala y autorícela de nuevo.", TipoError.CONFLICTO);
	}

	/** Mismo mensaje si el celular no existe o la contraseña está mal: no revela qué celulares tienen cuenta. */
	public static ErrorDominio credencialesInvalidas() {
		return new ErrorDominio("AUTH_CREDENCIALES_INVALIDAS", "El celular o la contraseña no coinciden.",
			TipoError.NO_AUTENTICADO);
	}

	public static ErrorDominio cuentaBloqueada(long minutos) {
		return new ErrorDominio("AUTH_CUENTA_BLOQUEADA",
			"Por seguridad bloqueamos su cuenta por demasiados intentos. Inténtelo de nuevo en " + minutos
				+ (minutos == 1 ? " minuto." : " minutos."),
			TipoError.BLOQUEADO);
	}

	public static ErrorDominio refreshInvalido() {
		return new ErrorDominio("AUTH_REFRESH_INVALIDO", "Su sesión terminó. Vuelva a iniciar sesión.",
			TipoError.NO_AUTENTICADO);
	}

	public static ErrorDominio usuarioNoEncontrado() {
		return new ErrorDominio("IDENTIDAD_USUARIO_NO_ENCONTRADO", "El usuario no existe.", TipoError.NO_ENCONTRADO);
	}
}
