package co.surco.clima.dominio;

import co.surco.compartido.error.ErrorDominio;
import co.surco.compartido.error.TipoError;

/** Errores del módulo. Los mensajes van de usted: los lee el agricultor. */
public final class ErroresClima {

	private ErroresClima() {
	}

	/** La fuente del pronóstico no respondió: el controlador contesta 503. */
	public static class ClimaNoDisponible extends RuntimeException {
		public ClimaNoDisponible(Throwable causa) {
			super("No pudimos consultar el clima en este momento. Inténtelo de nuevo en unos minutos.", causa);
		}
	}

	public static ErrorDominio ubicacionRequerida() {
		return new ErrorDominio("CLIMA_UBICACION_REQUERIDA", "Indique el municipio o las coordenadas (lat y lon).",
			TipoError.SOLICITUD_INVALIDA);
	}

	public static ErrorDominio fueraDeColombia() {
		return new ErrorDominio("CLIMA_FUERA_DE_COLOMBIA", "La ubicación está fuera de Colombia.",
			TipoError.SOLICITUD_INVALIDA);
	}

	public static ErrorDominio municipioNoEncontrado() {
		return new ErrorDominio("MUNICIPIO_NO_ENCONTRADO", "Ese municipio no existe.", TipoError.NO_ENCONTRADO);
	}
}
