package co.surco.clima.infraestructura.nasa;

import co.surco.clima.dominio.ErroresClima;
import co.surco.clima.dominio.Puertos.DatoDiario;
import co.surco.clima.dominio.Puertos.FuenteSueloYSol;
import co.surco.clima.dominio.Puertos.SueloYSol;
import co.surco.clima.dominio.Tarjetas;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * NASA POWER, dato diario: radiación solar (ALLSKY_SFC_SW_DWN, MJ/m²) y humedad de la zona de raíces
 * (GWETROOT, 0 a 1). Los días más recientes suelen llegar sin dato (-999): se toma el último que tenga.
 * El módulo alertas tiene su propio cliente de NASA POWER: los módulos no comparten infraestructura.
 */
@Component
class ClienteNasaPowerDiario implements FuenteSueloYSol {

	private static final String RADIACION = "ALLSKY_SFC_SW_DWN";
	private static final String SUELO = "GWETROOT";
	private static final double SIN_DATO = -999.0;
	private static final int DIAS_A_PEDIR = 15;
	private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
	private static final DateTimeFormatter AAAAMMDD = DateTimeFormatter.BASIC_ISO_DATE;

	private final RestClient http;

	ClienteNasaPowerDiario(RestClient.Builder builder) {
		var fabrica = new SimpleClientHttpRequestFactory();
		fabrica.setConnectTimeout(Duration.ofSeconds(5));
		fabrica.setReadTimeout(Duration.ofSeconds(20));
		this.http = builder.baseUrl("https://power.larc.nasa.gov/api/temporal").requestFactory(fabrica).build();
	}

	@Override
	public SueloYSol ultimos(double lat, double lon) {
		LocalDate hoy = LocalDate.now(COLOMBIA);
		Respuesta r;
		try {
			r = http.get().uri(uri -> uri.path("/daily/point")
				.queryParam("parameters", RADIACION + "," + SUELO)
				.queryParam("community", "AG")
				.queryParam("latitude", String.format(Locale.ROOT, "%.4f", lat))
				.queryParam("longitude", String.format(Locale.ROOT, "%.4f", lon))
				.queryParam("start", hoy.minusDays(DIAS_A_PEDIR).format(AAAAMMDD))
				.queryParam("end", hoy.format(AAAAMMDD))
				.queryParam("format", "JSON")
				.build()).retrieve().body(Respuesta.class);
		} catch (RestClientException e) {
			throw ErroresClima.fuenteNoDisponible(Tarjetas.NASA);
		}
		if (r == null || r.properties() == null || r.properties().parameter() == null) {
			throw ErroresClima.fuenteNoDisponible(Tarjetas.NASA);
		}
		Map<String, Map<String, Double>> p = r.properties().parameter();
		return new SueloYSol(ultimo(p.get(RADIACION)), ultimo(p.get(SUELO)));
	}

	/** El último día con dato. Las fechas vienen como AAAAMMDD: el orden de texto es el cronológico. */
	static DatoDiario ultimo(Map<String, Double> serie) {
		if (serie == null) {
			return null;
		}
		return serie.entrySet().stream()
			.filter(dia -> dia.getValue() != null && dia.getValue() > SIN_DATO)
			.max(Map.Entry.comparingByKey())
			.map(dia -> new DatoDiario(LocalDate.parse(dia.getKey(), AAAAMMDD), dia.getValue()))
			.orElse(null);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Respuesta(Propiedades properties) {
	}

	/** parameter = {"ALLSKY_SFC_SW_DWN": {"20261110": 20.2, ...}, "GWETROOT": {...}}. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record Propiedades(Map<String, Map<String, Double>> parameter) {
	}
}
