package co.surco.identidad.infraestructura.persistencia;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data de identidad.usuarios. Solo lo usa {@link AdaptadoresPostgres}. */
interface UsuariosJpa extends JpaRepository<UsuarioJpa, UUID> {

	Optional<UsuarioJpa> findByCelular(String celular);

	boolean existsByCelular(String celular);

	boolean existsByCorreo(String correo);
}
