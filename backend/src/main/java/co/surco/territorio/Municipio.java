package co.surco.territorio;

/**
 * Municipio de Colombia.
 *
 * @param codigo 5 dígitos DIVIPOLA; los dos primeros son el departamento (05001 = Medellín)
 * @param tipo   "Municipio", "Área no municipalizada" o "Isla", según el DANE
 * @param lat    latitud de la cabecera
 * @param lon    longitud de la cabecera
 */
public record Municipio(String codigo, String nombre, String tipo, Double lat, Double lon) {

	public String codigoDepartamento() {
		return codigo.substring(0, 2);
	}
}
