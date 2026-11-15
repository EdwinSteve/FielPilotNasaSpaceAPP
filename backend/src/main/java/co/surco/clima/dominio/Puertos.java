package co.surco.clima.dominio;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;

/** Puertos de salida del módulo: de dónde vienen los datos. Los adaptadores están en infraestructura. */
public final class Puertos {

	private Puertos() {
	}

	/** Condiciones actuales y pronóstico diario de un punto. Si falla, lanza una excepción. */
	public interface FuenteTiempo {
		Tiempo consultar(double lat, double lon);
	}

	/** Datos diarios de satélite y modelo (NASA POWER). Si falla, lanza una excepción. */
	public interface FuenteSatelital {
		DatosSatelite consultar(double lat, double lon);
	}

	/** Alerta hidrológica oficial vigente en el punto; vacío si no hay ninguna. */
	public interface FuenteAlertasRios {
		Optional<AlertaRio> vigenteEn(double lat, double lon);
	}

	/** @param dias pronóstico desde hoy */
	public record Tiempo(ResumenClima.Actual actual, List<ResumenClima.Dia> dias, Double altitudM) {
	}

	/** Un valor diario y el día al que corresponde. */
	public record ValorDiario(LocalDate fecha, double valor) {
	}

	/**
	 * @param radiacionMj     último dato de radiación solar, en MJ/m² por día; {@code null} si no hay
	 * @param humedadRaices   último dato de humedad del suelo en la zona de raíces, de 0 a 1
	 * @param lluviaDiaria    mm por día de las últimas semanas, solo días con dato
	 * @param lluviaNormalMes lluvia normal de cada mes, en mm por día
	 */
	public record DatosSatelite(ValorDiario radiacionMj, ValorDiario humedadRaices,
		SortedMap<LocalDate, Double> lluviaDiaria, Map<Month, Double> lluviaNormalMes) {
	}

	/**
	 * @param nivelIdeam tal como lo publica el IDEAM: "ALERTA AMARILLA", "ALERTA NARANJA" o "ALERTA ROJA"
	 * @param subzona    subzona hidrográfica, por ejemplo "Río Neiva"
	 */
	public record AlertaRio(String nivelIdeam, String subzona) {
	}
}
