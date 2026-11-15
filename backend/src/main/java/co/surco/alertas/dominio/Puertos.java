package co.surco.alertas.dominio;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;

/** Lo que las alertas necesitan del exterior. Las pruebas los reemplazan por versiones en memoria. */
public final class Puertos {

	private Puertos() {
	}

	/** Servidor GIS del IDEAM. Si no responde: ErrorDominio FUENTE_NO_DISPONIBLE. */
	public interface FuenteAlertasRios {

		/** Subzona hidrográfica con alerta vigente que contiene el punto. Vacío si la subzona no tiene alerta. */
		Optional<SubzonaConAlerta> conAlerta(double lat, double lon);

		/** true si el punto cae en alguna área hidrográfica del IDEAM, es decir, dentro de Colombia. */
		boolean dentroDeColombia(double lat, double lon);
	}

	/** {@code nivelIdeam}: valor de NIVEL_A ("ALERTA NARANJA"); {@code nombre}: NOMSZH. */
	public record SubzonaConAlerta(String nivelIdeam, String nombre) {
	}

	/** NASA POWER. Si no responde o sus datos están atrasados: ErrorDominio FUENTE_NO_DISPONIBLE. */
	public interface FuenteLluvia {
		Lluvia lluviaEn(double lat, double lon);
	}

	/** Lluvia de los últimos días (mm por día) y lluvia normal de cada mes (mm/día). */
	public record Lluvia(SortedMap<LocalDate, Double> diaria, Map<Month, Double> normalPorMes) {
	}
}
