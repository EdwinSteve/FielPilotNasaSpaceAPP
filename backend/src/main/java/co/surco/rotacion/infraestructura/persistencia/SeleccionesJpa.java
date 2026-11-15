package co.surco.rotacion.infraestructura.persistencia;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data de rotacion.selecciones. Solo lo usa {@link RotacionesPostgres.Selecciones}. */
interface SeleccionesJpa extends JpaRepository<SeleccionJpa, UUID> {

	Optional<SeleccionJpa> findByTerrenoIdAndUsuarioId(UUID terrenoId, UUID usuarioId);
}
