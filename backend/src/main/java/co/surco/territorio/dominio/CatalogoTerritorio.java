package co.surco.territorio.dominio;

import co.surco.territorio.Departamento;
import co.surco.territorio.Municipio;
import java.util.List;

/** Puerto: de dónde salen los departamentos y municipios (hoy, el JSON del DANE empaquetado en la app). */
public interface CatalogoTerritorio {

	/** Departamento con sus municipios. */
	record DepartamentoConMunicipios(Departamento departamento, List<Municipio> municipios) {
	}

	List<DepartamentoConMunicipios> cargar();
}
