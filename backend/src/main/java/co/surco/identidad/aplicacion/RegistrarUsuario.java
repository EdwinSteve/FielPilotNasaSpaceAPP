package co.surco.identidad.aplicacion;

import co.surco.identidad.dominio.Celular;
import co.surco.identidad.dominio.Correo;
import co.surco.identidad.dominio.ErroresIdentidad;
import co.surco.identidad.dominio.PoliticaContrasenas;
import co.surco.identidad.dominio.PoliticaDatos;
import co.surco.identidad.dominio.Puertos.CifradorContrasenas;
import co.surco.identidad.dominio.Puertos.RepositorioPoliticas;
import co.surco.identidad.dominio.Puertos.RepositorioUsuarios;
import co.surco.identidad.dominio.Puertos.ValidadorMunicipios;
import co.surco.identidad.dominio.Usuario;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Crea la cuenta del productor, guarda su consentimiento y deja la sesión iniciada. */
@Service
public class RegistrarUsuario {

	public record Comando(String nombreCompleto, String celular, String correo, String contrasena,
		String municipioResidencia, boolean aceptaPolitica, String versionPolitica) {
	}

	public record Resultado(Usuario usuario, Tokens tokens) {
	}

	private final RepositorioUsuarios usuarios;
	private final RepositorioPoliticas politicas;
	private final CifradorContrasenas cifrador;
	private final ValidadorMunicipios municipios;
	private final AbridorSesiones sesiones;
	private final Clock reloj;

	public RegistrarUsuario(RepositorioUsuarios usuarios, RepositorioPoliticas politicas, CifradorContrasenas cifrador,
		ValidadorMunicipios municipios, AbridorSesiones sesiones, Clock reloj) {
		this.usuarios = usuarios;
		this.politicas = politicas;
		this.cifrador = cifrador;
		this.municipios = municipios;
		this.sesiones = sesiones;
		this.reloj = reloj;
	}

	@Transactional
	public Resultado ejecutar(Comando c) {
		if (!c.aceptaPolitica()) {
			throw ErroresIdentidad.consentimientoRequerido();
		}
		PoliticaDatos vigente = politicas.vigente().orElseThrow(ErroresIdentidad::politicaDesactualizada);
		if (!vigente.version().equals(c.versionPolitica())) {
			throw ErroresIdentidad.politicaDesactualizada();
		}

		Celular celular = Celular.de(c.celular());
		Correo correo = Correo.deOpcional(c.correo());
		PoliticaContrasenas.validar(c.contrasena());
		if (!municipios.existe(c.municipioResidencia())) {
			throw ErroresIdentidad.municipioInexistente();
		}
		if (usuarios.existeCelular(celular)) {
			throw ErroresIdentidad.celularDuplicado();
		}
		if (correo != null && usuarios.existeCorreo(correo)) {
			throw ErroresIdentidad.correoDuplicado();
		}

		Instant ahora = reloj.instant();
		Usuario usuario = Usuario.nuevo(c.nombreCompleto(), celular, correo, cifrador.cifrar(c.contrasena()),
			c.municipioResidencia(), ahora);
		usuarios.guardar(usuario);
		politicas.registrarConsentimiento(usuario.id(), vigente.version(), ahora);
		return new Resultado(usuario, sesiones.abrir(usuario, ahora));
	}
}
