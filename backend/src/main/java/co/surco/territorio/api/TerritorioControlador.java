package co.surco.territorio.api;

import co.surco.compartido.error.ErrorDominio;
import co.surco.compartido.error.TipoError;
import co.surco.territorio.ConsultaTerritorio;
import co.surco.territorio.Departamento;
import co.surco.territorio.Municipio;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Departamentos y municipios. Público: el formulario de registro los necesita antes de tener cuenta. */
@RestController
@RequestMapping("/api/v1/territorio")
class TerritorioControlador {

	private final ConsultaTerritorio territorio;

	TerritorioControlador(ConsultaTerritorio territorio) {
		this.territorio = territorio;
	}

	@GetMapping("/departamentos")
	List<Departamento> departamentos() {
		return territorio.departamentos();
	}

	@GetMapping("/municipios")
	List<Municipio> municipios(@RequestParam String departamento) {
		return territorio.municipiosDe(departamento);
	}

	/** Sugerencia de municipio para un punto del mapa (la cabecera más cercana). 404 fuera de Colombia. */
	@GetMapping("/municipios/cercano")
	Municipio cercano(@RequestParam double lat, @RequestParam double lon) {
		if (lat < -4.3 || lat > 13.6 || lon < -82.0 || lon > -66.8) {
			throw new ErrorDominio("TERRITORIO_FUERA_DE_COLOMBIA", "Ese punto no queda en Colombia.", TipoError.NO_ENCONTRADO);
		}
		return territorio.municipioMasCercano(lat, lon).orElseThrow(
			() -> new ErrorDominio("TERRITORIO_FUERA_DE_COLOMBIA", "Ese punto no queda en Colombia.", TipoError.NO_ENCONTRADO));
	}
}
