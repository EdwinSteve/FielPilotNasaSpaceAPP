package co.surco.identidad.infraestructura.persistencia;

import co.surco.identidad.dominio.Celular;
import co.surco.identidad.dominio.Correo;
import co.surco.identidad.dominio.Rol;
import co.surco.identidad.dominio.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Tabla identidad.usuarios (migración V1). Solo la infraestructura conoce esta clase. */
@Entity
@Table(schema = "identidad", name = "usuarios")
class UsuarioJpa {

	@Id
	private UUID id;
	@Column(name = "nombre_completo", nullable = false)
	private String nombreCompleto;
	@Column(nullable = false, unique = true)
	private String celular;
	@Column(unique = true)
	private String correo;
	@Column(name = "contrasena_hash", nullable = false)
	private String contrasenaHash;
	@Column(name = "municipio_residencia", nullable = false)
	private String municipioResidencia;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Rol rol;
	@Column(name = "intentos_fallidos", nullable = false)
	private int intentosFallidos;
	@Column(name = "bloqueado_hasta")
	private Instant bloqueadoHasta;
	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	protected UsuarioJpa() {
	}

	static UsuarioJpa desde(Usuario u) {
		UsuarioJpa e = new UsuarioJpa();
		e.id = u.id();
		e.nombreCompleto = u.nombreCompleto();
		e.celular = u.celular().valor();
		e.correo = u.correo() == null ? null : u.correo().valor();
		e.contrasenaHash = u.contrasenaHash();
		e.municipioResidencia = u.municipioResidencia();
		e.rol = u.rol();
		e.intentosFallidos = u.intentosFallidos();
		e.bloqueadoHasta = u.bloqueadoHasta();
		e.creadoEn = u.creadoEn();
		return e;
	}

	Usuario aDominio() {
		return new Usuario(id, nombreCompleto, new Celular(celular), correo == null ? null : new Correo(correo),
			contrasenaHash, municipioResidencia, rol, creadoEn, intentosFallidos, bloqueadoHasta);
	}
}
