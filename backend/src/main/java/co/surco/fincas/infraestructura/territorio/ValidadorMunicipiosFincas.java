package co.surco.fincas.infraestructura.territorio;

import co.surco.fincas.dominio.Puertos.ValidadorMunicipios;
import co.surco.territorio.ConsultaTerritorio;
import org.springframework.stereotype.Component;

/**
 * Pregunta al módulo territorio (por su API pública) si el municipio del terreno existe.
 * Otro nombre que el de identidad: Spring nombra los beans por la clase y chocarían.
 */
@Component
class ValidadorMunicipiosFincas implements ValidadorMunicipios {

	private final ConsultaTerritorio territorio;

	ValidadorMunicipiosFincas(ConsultaTerritorio territorio) {
		this.territorio = territorio;
	}

	@Override
	public boolean existe(String codigoMunicipio) {
		return territorio.existeMunicipio(codigoMunicipio);
	}
}
