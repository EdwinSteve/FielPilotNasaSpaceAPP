package co.surco.rotacion.infraestructura.persistencia;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data de rotacion.planes. Solo lo usa {@link RotacionesPostgres.Planes}. */
interface PlanesJpa extends JpaRepository<PlanJpa, UUID> {

	Optional<PlanJpa> findFirstByTerrenoIdAndUsuarioIdOrderByCreadoEnDesc(UUID terrenoId, UUID usuarioId);

	Optional<PlanJpa> findByIdAndTerrenoIdAndUsuarioId(UUID id, UUID terrenoId, UUID usuarioId);
}
