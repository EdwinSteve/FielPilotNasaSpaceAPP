package co.surco.recomendacion.infraestructura.persistencia;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data de recomendacion.recomendaciones. Solo lo usa {@link RecomendacionesPostgres}. */
interface RecomendacionesJpa extends JpaRepository<RecomendacionJpa, UUID> {

	Optional<RecomendacionJpa> findByIdAndTerrenoIdAndUsuarioId(UUID id, UUID terrenoId, UUID usuarioId);

	Optional<RecomendacionJpa> findFirstByTerrenoIdAndUsuarioIdOrderByCreadaEnDesc(UUID terrenoId, UUID usuarioId);
}
