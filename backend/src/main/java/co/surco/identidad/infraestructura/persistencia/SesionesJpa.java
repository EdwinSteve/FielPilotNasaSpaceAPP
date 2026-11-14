package co.surco.identidad.infraestructura.persistencia;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data de identidad.sesiones. Solo lo usa {@link AdaptadoresPostgres}. */
interface SesionesJpa extends JpaRepository<SesionJpa, UUID> {

	/**
	 * SELECT ... FOR UPDATE: si llegan dos peticiones con el mismo refresh token a la vez,
	 * la segunda espera a que la primera lo marque como usado y entonces lo ve consumido.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<SesionJpa> findByHashToken(String hashToken);

	@Modifying(clearAutomatically = true)
	@Query("update SesionJpa s set s.revocadaEn = :ahora where s.familia = :familia and s.revocadaEn is null")
	int revocarFamilia(@Param("familia") UUID familia, @Param("ahora") Instant ahora);

	@Modifying(clearAutomatically = true)
	@Query("update SesionJpa s set s.revocadaEn = :ahora where s.usuarioId = :usuarioId and s.revocadaEn is null")
	int revocarDeUsuario(@Param("usuarioId") UUID usuarioId, @Param("ahora") Instant ahora);
}
