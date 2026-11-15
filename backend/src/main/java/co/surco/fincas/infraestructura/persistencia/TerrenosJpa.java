package co.surco.fincas.infraestructura.persistencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Spring Data de fincas.terrenos. Solo lo usa {@link AdaptadoresPostgres}. */
interface TerrenosJpa extends JpaRepository<TerrenoJpa, UUID> {

	List<TerrenoJpa> findByUsuarioIdOrderByCreadoEnAsc(UUID usuarioId);

	Optional<TerrenoJpa> findByIdAndUsuarioId(UUID id, UUID usuarioId);

	long countByUsuarioId(UUID usuarioId);

	@Modifying
	@Query("delete from TerrenoJpa t where t.id = :id and t.usuarioId = :usuarioId")
	int borrar(UUID id, UUID usuarioId);
}
