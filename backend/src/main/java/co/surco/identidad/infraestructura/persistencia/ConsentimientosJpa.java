package co.surco.identidad.infraestructura.persistencia;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data de identidad.consentimientos. */
interface ConsentimientosJpa extends JpaRepository<ConsentimientoJpa, UUID> {
}
