package co.surco.identidad.aplicacion;

import co.surco.identidad.dominio.ErroresIdentidad;
import co.surco.identidad.dominio.PoliticaDatos;
import co.surco.identidad.dominio.Puertos.RepositorioPoliticas;
import co.surco.identidad.dominio.Puertos.RepositorioUsuarios;
import co.surco.identidad.dominio.Usuario;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lecturas de identidad: el perfil del usuario autenticado y la política vigente. */
@Service
@Transactional(readOnly = true)
public class ConsultasIdentidad {

	private final RepositorioUsuarios usuarios;
	private final RepositorioPoliticas politicas;

	public ConsultasIdentidad(RepositorioUsuarios usuarios, RepositorioPoliticas politicas) {
		this.usuarios = usuarios;
		this.politicas = politicas;
	}

	public Usuario perfil(UUID usuarioId) {
		return usuarios.porId(usuarioId).orElseThrow(ErroresIdentidad::usuarioNoEncontrado);
	}

	public PoliticaDatos politicaVigente() {
		return politicas.vigente().orElseThrow(() -> new IllegalStateException("No hay una política de datos vigente"));
	}
}
