package co.surco.clima.infraestructura;

import static co.surco.clima.infraestructura.ClienteHttp.numero;

import co.surco.clima.dominio.Puertos.DatosSatelite;
import co.surco.clima.dominio.Puertos.FuenteSatelital;
import co.surco.clima.dominio.Puertos.ValorDiario;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * NASA POWER (https://power.larc.nasa.gov): radiación solar, humedad del suelo y lluvia diaria de las
 * últimas semanas, más la lluvia normal de cada mes (climatología que ya calcula la NASA).
 */
@Component
class NasaPowerCliente implements FuenteSatelital {

	private static final String BASE = "https://power.larc.nasa.gov/api/temporal";
	private static final String RADIACION = "ALLSKY_SFC_SW_DWN"; // MJ/m² por día
	private static final String SUELO = "GWETROOT"; // 0 a 1, zona de raíces
	private static final String LLUVIA = "PRECTOTCORR"; // mm por día
	private static final double SIN_DATO = -999.0;
	/** Se piden más de 30 días porque los más recientes suelen llegar sin dato. */
	private static final int DIAS_A_PEDIR = 45;
	private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
	private static final DateTimeFormatter AAAAMMDD = DateTimeFormatter.BASIC_ISO_DATE;
	private static final List<String> MESES = List.of("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP",
		"OCT", "NOV", "DEC");

	private final ClienteHttp http;
	/** La lluvia normal no cambia: se guarda una sola vez por zona. */
	private final Map<String, Map<Month, Double>> normales = new ConcurrentHashMap<>();

	NasaPowerCliente(ObjectMapper json) {
		this.http = new ClienteHttp(json, Duration.ofSeconds(15));
	}

	@Override
	public DatosSatelite consultar(double lat, double lon) {
		LocalDate hoy = LocalDate.now(COLOMBIA);
		JsonNode parametros = http.get(String.format(Locale.ROOT,
			"%s/daily/point?parameters=%s,%s,%s&community=AG&latitude=%.4f&longitude=%.4f&start=%s&end=%s&format=JSON",
			BASE, RADIACION, SUELO, LLUVIA, lat, lon, hoy.minusDays(DIAS_A_PEDIR).format(AAAAMMDD),
			hoy.format(AAAAMMDD))).path("properties").path("parameter");
		if (parametros.isMissingNode()) {
			throw new IllegalStateException("NASA POWER respondió sin datos diarios");
		}
		return new DatosSatelite(ultimo(serie(parametros.path(RADIACION))), ultimo(serie(parametros.path(SUELO))),
			serie(parametros.path(LLUVIA)), lluviaNormal(lat, lon));
	}

	/** La celda de NASA POWER mide unos 50 km: redondear a 0,1° deja que terrenos vecinos compartan el dato. */
	private Map<Month, Double> lluviaNormal(double lat, double lon) {
		String zona = String.format(Locale.ROOT, "%.1f,%.1f", lat, lon);
		Map<Month, Double> guardada = normales.get(zona);
		if (guardada != null) {
			return guardada;
		}
		try {
			JsonNode meses = http.get(String.format(Locale.ROOT,
				"%s/climatology/point?parameters=%s&community=AG&latitude=%.4f&longitude=%.4f&format=JSON", BASE, LLUVIA,
				lat, lon)).path("properties").path("parameter").path(LLUVIA);
			Map<Month, Double> normal = new EnumMap<>(Month.class);
			for (int i = 0; i < MESES.size(); i++) {
				Double mm = numero(meses.path(MESES.get(i)));
				if (mm != null && mm > SIN_DATO) {
					normal.put(Month.of(i + 1), mm);
				}
			}
			if (!normal.isEmpty()) {
				normales.put(zona, normal);
			}
			return normal;
		} catch (RuntimeException e) {
			// Sin la lluvia normal no hay indicador de sequía, pero la radiación y el suelo sí sirven
			return Map.of();
		}
	}

	/** { "20261101": 3.2, ... } a fechas, sin los días que llegan en -999. */
	private static SortedMap<LocalDate, Double> serie(JsonNode crudo) {
		SortedMap<LocalDate, Double> serie = new TreeMap<>();
		for (Map.Entry<String, JsonNode> dia : crudo.properties()) {
			Double valor = numero(dia.getValue());
			if (valor != null && valor > SIN_DATO) {
				serie.put(LocalDate.parse(dia.getKey(), AAAAMMDD), valor);
			}
		}
		return serie;
	}

	private static ValorDiario ultimo(SortedMap<LocalDate, Double> serie) {
		return serie.isEmpty() ? null : new ValorDiario(serie.lastKey(), serie.get(serie.lastKey()));
	}
}
