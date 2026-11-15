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

	/**
	 * El municipio cuya cabecera queda más cerca del punto. Es una sugerencia (cerca de un límite
	 * municipal puede fallar): el productor la confirma en el formulario.
	 */
	default Optional<Municipio> municipioMasCercano(double lat, double lon) {
		double coseno = Math.cos(Math.toRadians(lat));
		Municipio mejor = null;
		double menor = Double.MAX_VALUE;
		for (Departamento d : departamentos()) {
			for (Municipio m : municipiosDe(d.codigo())) {
				if (m.lat() == null || m.lon() == null) {
					continue;
				}
				double dy = m.lat() - lat;
				double dx = (m.lon() - lon) * coseno;
				double distancia = dx * dx + dy * dy;
				if (distancia < menor) {
					menor = distancia;
					mejor = m;
				}
			}
		}
		return Optional.ofNullable(mejor);
	}
}
