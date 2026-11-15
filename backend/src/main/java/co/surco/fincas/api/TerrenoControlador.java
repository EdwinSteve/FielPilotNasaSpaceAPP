package co.surco.fincas.api;

import co.surco.compartido.seguridad.UsuarioAutenticado;
import co.surco.fincas.aplicacion.ConsultasTerrenos;
import co.surco.fincas.aplicacion.EliminarTerreno;
import co.surco.fincas.aplicacion.RegistrarTerreno;
import co.surco.fincas.api.Dtos.RespuestaTerreno;
import co.surco.fincas.api.Dtos.SolicitudTerreno;
import co.surco.fincas.api.Dtos.Vertice;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Terrenos del productor autenticado. Protegidas (no están en RutasPublicas): cada productor solo ve
 * y borra los suyos, y uno ajeno responde 404 igual que uno inexistente.
 */
@RestController
@RequestMapping("/api/v1/fincas/terrenos")
class TerrenoControlador {

	private final RegistrarTerreno registrar;
	private final ConsultasTerrenos consultas;
	private final EliminarTerreno eliminar;

	TerrenoControlador(RegistrarTerreno registrar, ConsultasTerrenos consultas, EliminarTerreno eliminar) {
		this.registrar = registrar;
		this.consultas = consultas;
		this.eliminar = eliminar;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	RespuestaTerreno crear(@AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody SolicitudTerreno s) {
		var solicitud = new RegistrarTerreno.Solicitud(s.nombre(), s.municipio(), s.riego(),
			s.vertices().stream().map(Vertice::aDominio).toList());
		return RespuestaTerreno.de(registrar.ejecutar(usuario.id(), solicitud));
	}

	@GetMapping
	List<RespuestaTerreno> misTerrenos(@AuthenticationPrincipal UsuarioAutenticado usuario) {
		return consultas.de(usuario.id()).stream().map(RespuestaTerreno::de).toList();
	}

	@GetMapping("/{id}")
	RespuestaTerreno uno(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
		return RespuestaTerreno.de(consultas.obtener(usuario.id(), id));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void borrar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable UUID id) {
		eliminar.ejecutar(usuario.id(), id);
	}
}
