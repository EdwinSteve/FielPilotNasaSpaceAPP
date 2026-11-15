package co.surco.recomendacion.dominio;

import co.surco.compartido.error.ErrorDominio;
import co.surco.compartido.error.TipoError;

/** Errores de recomendación. Los códigos son el contrato con frontend/src/modulos/recomendacion/tipos.ts. */
public final class ErroresRecomendacion {

	private ErroresRecomendacion() {
	}

	public static ErrorDominio terrenoNoEncontrado() {
		return new ErrorDominio("RECOMENDACION_TERRENO_NO_ENCONTRADO", "Ese terreno no existe.", TipoError.NO_ENCONTRADO);
	}

	public static ErrorDominio modeloNoDisponible() {
		return new ErrorDominio("RECOMENDACION_NO_DISPONIBLE",
			"El modelo de recomendación no está disponible en este momento. Inténtelo de nuevo en unos minutos.",
			TipoError.NO_DISPONIBLE);
	}

	public static ErrorDominio terrenoSinDatos(String detalle) {
		return new ErrorDominio("RECOMENDACION_TERRENO_SIN_DATOS",
			"No encontramos datos de clima o suelo suficientes para ese terreno"
				+ (detalle == null || detalle.isBlank() ? "." : ": " + detalle),
			TipoError.SOLICITUD_INVALIDA);
	}
}
