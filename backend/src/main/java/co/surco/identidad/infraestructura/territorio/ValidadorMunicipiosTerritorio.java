package co.surco.identidad.infraestructura.territorio;

import co.surco.identidad.dominio.Puertos.ValidadorMunicipios;
import co.surco.territorio.ConsultaTerritorio;
import org.springframework.stereotype.Component;

/** Pregunta al módulo territorio (por su API pública) si el municipio existe. */
@Component
class ValidadorMunicipiosTerritorio implements ValidadorMunicipios {

	private final ConsultaTerritorio territorio;

	ValidadorMunicipiosTerritorio(ConsultaTerritorio territorio) {
		this.territorio = territorio;
	}

	@Override
	public boolean existe(String codigoMunicipio) {
		return territorio.existeMunicipio(codigoMunicipio);
	}
}
