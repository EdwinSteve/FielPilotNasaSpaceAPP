package co.surco.territorio.infraestructura;

import co.surco.territorio.Departamento;
import co.surco.territorio.Municipio;
import co.surco.territorio.dominio.CatalogoTerritorio;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Lee {@code territorio/divipola.json}: los 33 departamentos y 1.122 municipios del DANE
 * (datos.gov.co, conjunto gdxc-w37w). Se regenera con {@code scripts/actualizar_divipola.py}.
 */
@Component
class CatalogoDivipolaJson implements CatalogoTerritorio {

	static final String RUTA = "territorio/divipola.json";

	private final ObjectMapper json;

	CatalogoDivipolaJson(ObjectMapper json) {
		this.json = json;
	}

	record ArchivoDivipola(String fuente, String descargado, List<DepartamentoJson> departamentos) {
	}

	record DepartamentoJson(String codigo, String nombre, List<Municipio> municipios) {
	}

	@Override
	public List<DepartamentoConMunicipios> cargar() {
		try (InputStream entrada = new ClassPathResource(RUTA).getInputStream()) {
			ArchivoDivipola archivo = json.readValue(entrada, ArchivoDivipola.class);
			return archivo.departamentos().stream()
				.map(d -> new DepartamentoConMunicipios(new Departamento(d.codigo(), d.nombre()), d.municipios()))
				.toList();
		} catch (IOException e) {
			throw new UncheckedIOException("No se pudo leer " + RUTA, e);
		}
	}
}
