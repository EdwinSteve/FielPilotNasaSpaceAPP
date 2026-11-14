package co.surco.territorio.aplicacion;

import co.surco.compartido.error.ErrorDominio;
import co.surco.compartido.error.TipoError;
import co.surco.territorio.ConsultaTerritorio;
import co.surco.territorio.Departamento;
import co.surco.territorio.Municipio;
import co.surco.territorio.dominio.CatalogoTerritorio;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Implementa la API pública del módulo. El catálogo se carga una vez al arrancar y queda en memoria. */
@Service
public class ServicioTerritorio implements ConsultaTerritorio {

	private final List<Departamento> departamentos;
	private final Map<String, List<Municipio>> municipiosPorDepartamento = new HashMap<>();
	private final Map<String, Municipio> municipiosPorCodigo = new HashMap<>();

	public ServicioTerritorio(CatalogoTerritorio catalogo) {
		List<CatalogoTerritorio.DepartamentoConMunicipios> datos = catalogo.cargar();
		this.departamentos = datos.stream().map(CatalogoTerritorio.DepartamentoConMunicipios::departamento).toList();
		for (var d : datos) {
			municipiosPorDepartamento.put(d.departamento().codigo(), List.copyOf(d.municipios()));
			d.municipios().forEach(m -> municipiosPorCodigo.put(m.codigo(), m));
		}
	}

	@Override
	public List<Departamento> departamentos() {
		return departamentos;
	}

	@Override
	public List<Municipio> municipiosDe(String codigoDepartamento) {
		List<Municipio> municipios = municipiosPorDepartamento.get(codigoDepartamento);
		if (municipios == null) {
			throw new ErrorDominio("DEPARTAMENTO_NO_ENCONTRADO", "Ese departamento no existe.", TipoError.NO_ENCONTRADO);
		}
		return municipios;
	}

	@Override
	public Optional<Municipio> municipio(String codigoMunicipio) {
		return Optional.ofNullable(municipiosPorCodigo.get(codigoMunicipio));
	}
}
