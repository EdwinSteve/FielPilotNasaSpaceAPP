package co.surco.territorio.api;

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
}
