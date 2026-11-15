package co.surco.clima.dominio;

import co.surco.compartido.error.ErrorDominio;
import co.surco.compartido.error.TipoError;

/** Errores de clima. Mismo código que alertas para una ubicación fuera de Colombia. */
public final class ErroresClima {

	private ErroresClima() {
	}

	public static ErrorDominio fueraDeColombia() {
		return new ErrorDominio("UBICACION_FUERA_DE_COLOMBIA", "La ubicación está fuera de Colombia.",
			TipoError.SOLICITUD_INVALIDA);
	}

	/** {@code fuente}: "Google Weather" o "NASA POWER". Solo se registra: la tarjeta sale sin dato. */
	public static ErrorDominio fuenteNoDisponible(String fuente) {
		return new ErrorDominio("FUENTE_NO_DISPONIBLE",
			"No pudimos consultar " + fuente + ". Inténtelo de nuevo en unos minutos.", TipoError.NO_DISPONIBLE);
	}
}
