package co.surco.fincas.dominio;

import co.surco.compartido.error.ErrorDominio;
import co.surco.compartido.error.TipoError;

/**
 * Errores de negocio de fincas. Los códigos son el contrato con el frontend
 * ({@code frontend/src/modulos/fincas/tipos.ts}); no los cambie sin cambiarlos allá.
 * Los nombres de campo son los del JSON del formulario.
 */
public final class ErroresFincas {

	/** Un productor con más terrenos que esto casi seguro está probando o es un error. */
	public static final int MAXIMO_TERRENOS = 50;

	private ErroresFincas() {
	}

	public static ErrorDominio nombreInvalido() {
		return ErrorDominio.deCampo("FINCAS_NOMBRE_INVALIDO", "nombre",
			"Póngale un nombre a su terreno (máximo " + Terreno.LARGO_MAXIMO_NOMBRE + " letras), por ejemplo \"Lote de arriba\".");
	}

	public static ErrorDominio municipioInexistente() {
		return ErrorDominio.deCampo("FINCAS_MUNICIPIO_INEXISTENTE", "municipio", "Elija un municipio de la lista.");
	}

	public static ErrorDominio coordenadaInvalida() {
		return ErrorDominio.deCampo("FINCAS_COORDENADA_INVALIDA", "vertices",
			"Uno de los puntos del contorno no es válido. Bórrelo y márquelo de nuevo en el mapa.");
	}

	public static ErrorDominio muyPocosVertices() {
		return ErrorDominio.deCampo("FINCAS_POLIGONO_INCOMPLETO", "vertices",
			"Marque al menos " + Poligono.MINIMO_VERTICES + " puntos en el mapa, uno en cada esquina del terreno.");
	}

	public static ErrorDominio demasiadosVertices() {
		return ErrorDominio.deCampo("FINCAS_POLIGONO_DEMASIADOS_PUNTOS", "vertices",
			"El contorno tiene demasiados puntos (máximo " + Poligono.MAXIMO_VERTICES + "). Marque solo las esquinas.");
	}

	public static ErrorDominio fueraDeColombia() {
		return ErrorDominio.deCampo("FINCAS_FUERA_DE_COLOMBIA", "vertices",
			"El terreno debe estar en Colombia. Revise los puntos que marcó en el mapa.");
	}

	public static ErrorDominio ladosCruzados() {
		return ErrorDominio.deCampo("FINCAS_POLIGONO_CRUZADO", "vertices",
			"Los lados del contorno se cruzan. Marque las esquinas en orden, siguiendo el borde del terreno.");
	}

	public static ErrorDominio areaMuyPequena() {
		return ErrorDominio.deCampo("FINCAS_AREA_MUY_PEQUENA", "vertices",
			"El terreno marcado es muy pequeño (menos de 100 m²). Acerque el mapa y marque las esquinas reales.");
	}

	public static ErrorDominio areaMuyGrande() {
		return ErrorDominio.deCampo("FINCAS_AREA_MUY_GRANDE", "vertices",
			"El terreno marcado es demasiado grande (más de 20.000 hectáreas). Revise los puntos del contorno.");
	}

	public static ErrorDominio limiteTerrenos() {
		return new ErrorDominio("FINCAS_LIMITE_TERRENOS",
			"Ya registró " + MAXIMO_TERRENOS + " terrenos, el máximo por cuenta.", TipoError.CONFLICTO);
	}

	/** También para terrenos de otro productor: no revela que existen. */
	public static ErrorDominio terrenoNoEncontrado() {
		return new ErrorDominio("FINCAS_TERRENO_NO_ENCONTRADO", "Ese terreno no existe.", TipoError.NO_ENCONTRADO);
	}
}
