package co.surco.territorio;

import java.util.List;
import java.util.Optional;

/**
 * API pública del módulo territorio: departamentos y municipios de Colombia (DIVIPOLA del DANE).
 * Otros módulos solo usan esta interfaz y los records de este paquete, nunca las subcapas.
 */
public interface ConsultaTerritorio {

	/** Los 33 departamentos (incluye Bogotá D.C.), en orden alfabético. */
	List<Departamento> departamentos();

	/** Municipios del departamento, en orden alfabético. Lanza NO_ENCONTRADO si el departamento no existe. */
	List<Municipio> municipiosDe(String codigoDepartamento);

	Optional<Municipio> municipio(String codigoMunicipio);

	default boolean existeMunicipio(String codigoMunicipio) {
		return codigoMunicipio != null && municipio(codigoMunicipio).isPresent();
	}
}
