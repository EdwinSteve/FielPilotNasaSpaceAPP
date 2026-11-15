package co.surco.alertas.dominio;

import co.surco.compartido.error.ErrorDominio;
import co.surco.compartido.error.TipoError;

/** Errores de alertas. Los códigos son el contrato con frontend/src/modulos/alertas (respuestas.ts). */
public final class ErroresAlertas {

	private ErroresAlertas() {
	}

	public static ErrorDominio fueraDeColombia() {
		return new ErrorDominio("UBICACION_FUERA_DE_COLOMBIA", "La ubicación está fuera de Colombia.",
			TipoError.SOLICITUD_INVALIDA);
	}

	/** No se asume NORMAL ante un nivel que no conocemos: sería esconder una alerta. */
	public static ErrorDominio nivelDesconocido() {
		return new ErrorDominio("NIVEL_DESCONOCIDO", "La fuente envió un nivel de alerta desconocido.",
			TipoError.FUENTE_INVALIDA);
	}

	/** {@code fuente}: "el IDEAM" o "la NASA". */
	public static ErrorDominio fuenteNoDisponible(String fuente) {
		return new ErrorDominio("FUENTE_NO_DISPONIBLE",
			"No pudimos consultar " + fuente + ". Inténtelo de nuevo en unos minutos.", TipoError.NO_DISPONIBLE);
	}
}
