package co.surco.identidad.infraestructura.persistencia;

import co.surco.identidad.dominio.PoliticaDatos;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla identidad.politicas_datos. Una sola fila tiene vigente = true (índice único parcial). */
@Entity
@Table(schema = "identidad", name = "politicas_datos")
class PoliticaJpa {

	@Id
	private String version;
	@Column(name = "url_texto", nullable = false)
	private String urlTexto;
	@Column(nullable = false)
	private String contenido;
	@Column(nullable = false)
	private boolean vigente;

	protected PoliticaJpa() {
	}

	PoliticaDatos aDominio() {
		return new PoliticaDatos(version, urlTexto, contenido);
	}
}
