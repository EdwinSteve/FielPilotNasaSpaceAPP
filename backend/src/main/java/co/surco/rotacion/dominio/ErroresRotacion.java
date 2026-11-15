package co.surco.rotacion.dominio;

import co.surco.compartido.error.ErrorDominio;
import co.surco.compartido.error.TipoError;

/** Errores de rotación. Los códigos son el contrato con frontend/src/modulos/rotacion/api.ts. */
public final class ErroresRotacion {

	private ErroresRotacion() {
	}

	public static ErrorDominio sinCalcular() {
		return new ErrorDominio("ROTACION_SIN_CALCULAR", "Todavía no hemos calculado rotaciones para ese lote.",
			TipoError.NO_ENCONTRADO);
	}

	public static ErrorDominio rotacionNoEncontrada() {
		return new ErrorDominio("ROTACION_NO_ENCONTRADA",
			"Esa rotación ya no está en el plan del lote. Actualice el plan y elija de nuevo.", TipoError.NO_ENCONTRADO);
	}

	public static ErrorDominio sinSeleccion() {
		return new ErrorDominio("ROTACION_SIN_SELECCION", "Todavía no ha elegido un ciclo para ese lote.",
			TipoError.NO_ENCONTRADO);
	}
}
