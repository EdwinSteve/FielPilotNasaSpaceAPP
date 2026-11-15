package co.surco.clima.dominio;

import java.time.LocalDate;

/** Lo que el clima necesita del exterior. Las pruebas los reemplazan por versiones en memoria. */
public final class Puertos {

	private Puertos() {
	}

	/** Weather API de Google: condiciones actuales del punto. Si falla, lanza una excepción. */
	public interface FuenteClimaAhora {
		ClimaAhora actual(double lat, double lon);
	}

	/** NASA POWER: último dato diario de radiación solar y humedad del suelo. Si falla, lanza una excepción. */
	public interface FuenteSueloYSol {
		SueloYSol ultimos(double lat, double lon);
	}

	/**
	 * Condiciones actuales. Cualquier campo puede ser null si la fuente no lo envía.
	 *
	 * @param medido fecha y hora ISO en hora de Colombia
	 */
	public record ClimaAhora(Double temperaturaC, Double cambio24hC, Double lluvia24hMm, Double lluviaUltimaHoraMm,
		Integer probabilidadLluvia, Integer humedadRelativa, Double puntoRocioC, Double vientoKmh,
		Integer direccionVientoGrados, Double rafagasKmh, Integer indiceUv, String medido) {
	}

	/** Un valor diario y el día al que corresponde. */
	public record DatoDiario(LocalDate fecha, double valor) {
	}

	/**
	 * @param radiacionMj     radiación solar del día, en MJ/m² (null si no hay dato)
	 * @param humedadSuelo    humedad en la zona de raíces, de 0 a 1 (null si no hay dato)
	 */
	public record SueloYSol(DatoDiario radiacionMj, DatoDiario humedadSuelo) {
	}
}
