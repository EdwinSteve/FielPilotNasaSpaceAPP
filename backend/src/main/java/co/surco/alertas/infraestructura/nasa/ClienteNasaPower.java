package co.surco.alertas.infraestructura.nasa;

import co.surco.alertas.dominio.ErroresAlertas;
import co.surco.alertas.dominio.Puertos.FuenteLluvia;
import co.surco.alertas.dominio.Puertos.Lluvia;
import co.surco.compartido.error.ErrorDominio;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Lluvia de NASA POWER (parámetro PRECTOTCORR, mm/día). Dos consultas pequeñas y en paralelo en vez de
 * bajar décadas de serie diaria: los últimos 45 días y la climatología mensual ya calculada por la NASA.
 * Guarda el resultado por zona (0,1°, unos 11 km; la celda de la NASA mide unos 50 km) y por día.
 */
@Component
class ClienteNasaPower implements FuenteLluvia {

	private static final Logger LOG = LoggerFactory.getLogger(ClienteNasaPower.class);
	private static final String BASE = "https://power.larc.nasa.gov/api/temporal";
	private static final String LLUVIA = "PRECTOTCORR";
	private static final double SIN_DATO = -999.0;
	private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
	private static final DateTimeFormatter AAAAMMDD = DateTimeFormatter.BASIC_ISO_DATE;
	private static final List<String> MESES = List.of(
		"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");
	/** Se piden más de 30 días porque los más recientes suelen llegar sin dato. */
	private static final int DIAS_A_PEDIR = 45;
	/** Si el último dato es más viejo que esto, no sirve para avisar hoy. */
	private static final int DIAS_MAXIMOS_DE_RETRASO = 10;

	private final RestClient http;
	private final Map<String, EnCache> cache = new ConcurrentHashMap<>();
	/** La lluvia normal no cambia: se pide una sola vez por zona. */
	private final Map<String, Map<Month, Double>> normales = new ConcurrentHashMap<>();

	private record EnCache(LocalDate dia, Lluvia lluvia) {
	}

	ClienteNasaPower(RestClient.Builder builder) {
		var fabrica = new SimpleClientHttpRequestFactory();
		fabrica.setConnectTimeout(Duration.ofSeconds(5));
		fabrica.setReadTimeout(Duration.ofSeconds(20));
		this.http = builder.baseUrl(BASE).requestFactory(fabrica).build();
	}

	@Override
	public Lluvia lluviaEn(double lat, double lon) {
		LocalDate hoy = LocalDate.now(COLOMBIA);
		double latZona = Math.round(lat * 10) / 10.0;
		double lonZona = Math.round(lon * 10) / 10.0;
		String clave = String.format(Locale.ROOT, "%.1f,%.1f", latZona, lonZona);

		EnCache guardado = cache.get(clave);
		if (guardado != null && guardado.dia().equals(hoy)) {
			return guardado.lluvia();
		}

		var diaria = CompletableFuture.supplyAsync(() -> diaria(latZona, lonZona, hoy.minusDays(DIAS_A_PEDIR), hoy));
		var normal = CompletableFuture.supplyAsync(
			() -> normales.computeIfAbsent(clave, c -> normalPorMes(latZona, lonZona)));
		Lluvia lluvia;
		try {
			lluvia = new Lluvia(diaria.join(), normal.join());
		} catch (CompletionException e) {
			if (e.getCause() instanceof ErrorDominio error) {
				throw error;
			}
			LOG.error("Falló NASA POWER", e.getCause());
			throw ErroresAlertas.fuenteNoDisponible("la lluvia de la NASA");
		}

		if (lluvia.diaria().isEmpty() || lluvia.diaria().lastKey().isBefore(hoy.minusDays(DIAS_MAXIMOS_DE_RETRASO))) {
			// No se guarda: la próxima petición vuelve a consultar
			LOG.warn("NASA POWER no tiene lluvia reciente para {}", clave);
			throw ErroresAlertas.fuenteNoDisponible("la lluvia de la NASA");
		}
		cache.put(clave, new EnCache(hoy, lluvia));
		return lluvia;
	}

	/** mm por día; los días sin dato (-999) no se incluyen. */
	private SortedMap<LocalDate, Double> diaria(double lat, double lon, LocalDate inicio, LocalDate fin) {
		SortedMap<LocalDate, Double> lluvia = new TreeMap<>();
		consultar("/daily/point", lat, lon, inicio, fin).forEach((fecha, mm) -> {
			if (mm != null && mm > SIN_DATO) {
				lluvia.put(LocalDate.parse(fecha, AAAAMMDD), mm);
			}
		});
		return lluvia;
	}

	/** Lluvia normal de cada mes en mm/día. */
	private Map<Month, Double> normalPorMes(double lat, double lon) {
		Map<String, Double> crudo = consultar("/climatology/point", lat, lon, null, null);
		Map<Month, Double> normal = new EnumMap<>(Month.class);
		for (int i = 0; i < MESES.size(); i++) {
			Double mm = crudo.get(MESES.get(i));
			if (mm != null && mm > SIN_DATO) {
				normal.put(Month.of(i + 1), mm);
			}
		}
		return normal;
	}

	private Map<String, Double> consultar(String ruta, double lat, double lon, LocalDate inicio, LocalDate fin) {
		Respuesta r;
		try {
			r = http.get().uri(uri -> {
				uri.path(ruta)
					.queryParam("parameters", LLUVIA)
					.queryParam("community", "AG")
					.queryParam("latitude", String.format(Locale.ROOT, "%.4f", lat))
					.queryParam("longitude", String.format(Locale.ROOT, "%.4f", lon))
					.queryParam("format", "JSON");
				if (inicio != null && fin != null) {
					uri.queryParam("start", inicio.format(AAAAMMDD)).queryParam("end", fin.format(AAAAMMDD));
				}
				return uri.build();
			}).retrieve().body(Respuesta.class);
		} catch (RestClientException e) {
			LOG.error("NASA POWER no responde en {}", ruta, e);
			throw ErroresAlertas.fuenteNoDisponible("la lluvia de la NASA");
		}
		if (r == null || r.properties() == null || r.properties().parameter() == null
			|| r.properties().parameter().get(LLUVIA) == null) {
			LOG.error("NASA POWER respondió sin datos de lluvia en {}", ruta);
			throw ErroresAlertas.fuenteNoDisponible("la lluvia de la NASA");
		}
		return r.properties().parameter().get(LLUVIA);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Respuesta(Propiedades properties) {
	}

	/** parameter = {"PRECTOTCORR": {"20261101": 3.2, ...}} o {"PRECTOTCORR": {"JAN": 2.1, ...}}. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	record Propiedades(Map<String, Map<String, Double>> parameter) {
	}
}
