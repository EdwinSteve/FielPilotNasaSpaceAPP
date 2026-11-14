package co.surco.identidad.infraestructura.persistencia;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data de identidad.politicas_datos. */
interface PoliticasJpa extends JpaRepository<PoliticaJpa, String> {

	Optional<PoliticaJpa> findByVigenteTrue();
}
