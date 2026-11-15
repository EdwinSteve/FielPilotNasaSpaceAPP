package co.surco.recomendacion.dominio;

import co.surco.compartido.error.ErrorCampo;
import co.surco.compartido.error.ErrorDominio;
import co.surco.compartido.error.TipoError;
import java.util.List;

/**
 * Errores de recomendación. Los códigos son el contrato con frontend/src/modulos/recomendacion/tipos.ts
 * y frontend/src/modulos/rotacion/api.ts.
 */
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

	public static ErrorDominio recomendacionNoEncontrada() {
		return new ErrorDominio("RECOMENDACION_NO_ENCONTRADA",
			"Todavía no hay una recomendación calculada para ese terreno.", TipoError.NO_ENCONTRADO);
	}

	/** SoilGrids no tiene un perfil de suelo cerca del terreno: el modelo no puede evaluarlo. */
	public static ErrorDominio terrenoSinSuelo() {
		return new ErrorDominio("RECOMENDACION_TERRENO_SIN_SUELO",
			"No tenemos datos del suelo de ese terreno, así que no podemos recomendarle cultivos todavía.",
			TipoError.NO_PROCESABLE);
	}

	public static ErrorDominio mesNoEntrenado() {
		return new ErrorDominio("RECOMENDACION_MES_NO_ENTRENADO",
			"El modelo todavía no sabe recomendar siembras para esa época del año.", TipoError.NO_PROCESABLE);
	}

	/** Una fuente de datos (NASA POWER, SoilGrids...) no respondió. {@code fuente} va en los detalles. */
	public static ErrorDominio fuenteCaida(String fuente) {
		String nombre = fuente == null || fuente.isBlank() ? "una fuente de datos" : fuente;
		return new ErrorDominio("RECOMENDACION_FUENTE_CAIDA",
			"No pudimos consultar " + nombre + ". Inténtelo de nuevo en unos minutos.", TipoError.FUENTE_EXTERNA,
			List.of(new ErrorCampo("fuente", nombre)));
	}

	public static ErrorDominio siembraInvalida() {
		return ErrorDominio.deCampo("RECOMENDACION_SIEMBRA_INVALIDA", "siembra",
			"La fecha de siembra debe estar entre hoy y los próximos 60 días.");
	}
}
