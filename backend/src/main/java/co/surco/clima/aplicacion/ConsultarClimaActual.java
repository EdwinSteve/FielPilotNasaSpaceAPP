package co.surco.clima.aplicacion;

import co.surco.clima.dominio.Condicion;
import co.surco.clima.dominio.ErroresClima;
import co.surco.clima.dominio.Puertos.ClimaAhora;
import co.surco.clima.dominio.Puertos.FuenteClimaAhora;
import co.surco.clima.dominio.Puertos.FuenteSueloYSol;
import co.surco.clima.dominio.Puertos.SueloYSol;
import co.surco.clima.dominio.Tarjetas;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Condiciones actuales de un punto. Google y la NASA se consultan al mismo tiempo; si una falla, solo sus
 * tarjetas salen sin dato. Sin transacción: no escribe en la base.
 */
@Service
public class ConsultarClimaActual {

	private static final Logger LOG = LoggerFactory.getLogger(ConsultarClimaActual.class);
	private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
	/** Evita gastar una consulta de Google por cada recarga del tablero. */
	private static final Duration VIGENCIA_CACHE = Duration.ofMinutes(10);

	private final FuenteClimaAhora google;
	private final FuenteSueloYSol nasa;
	private final Map<String, EnCache> cache = new ConcurrentHashMap<>();

	private record EnCache(Instant momento, List<Condicion> condiciones) {
	}

	public ConsultarClimaActual(FuenteClimaAhora google, FuenteSueloYSol nasa) {
		this.google = google;
		this.nasa = nasa;
	}

	public record ClimaActual(double lat, double lon, OffsetDateTime consultado, List<Condicion> condiciones) {
	}

	public ClimaActual en(double lat, double lon) {
		boolean dentro = lat >= -4.5 && lat <= 13.5 && lon >= -82 && lon <= -66; // también rechaza NaN
		if (!dentro) {
			throw ErroresClima.fueraDeColombia();
		}
		String clave = String.format(Locale.ROOT, "%.2f,%.2f", lat, lon);
		EnCache guardado = cache.get(clave);
		List<Condicion> condiciones;
		if (guardado != null && guardado.momento().plus(VIGENCIA_CACHE).isAfter(Instant.now())) {
			condiciones = guardado.condiciones();
		} else {
			condiciones = consultar(lat, lon);
			// Solo se guarda si al menos una tarjeta trajo dato
			if (condiciones.stream().anyMatch(Condicion::tieneDato)) {
				cache.put(clave, new EnCache(Instant.now(), condiciones));
			}
		}
		return new ClimaActual(lat, lon, OffsetDateTime.now(COLOMBIA).truncatedTo(ChronoUnit.SECONDS), condiciones);
	}

	private List<Condicion> consultar(double lat, double lon) {
		CompletableFuture<ClimaAhora> ahora = CompletableFuture
			.supplyAsync(() -> seguro(Tarjetas.GOOGLE, () -> google.actual(lat, lon)));
		CompletableFuture<SueloYSol> sueloYSol = CompletableFuture
			.supplyAsync(() -> seguro(Tarjetas.NASA, () -> nasa.ultimos(lat, lon)));
		return Tarjetas.armar(ahora.join(), sueloYSol.join());
	}

	/** Si la fuente falla, devuelve null: sus tarjetas salen sin dato en vez de tumbar el endpoint. */
	private static <T> T seguro(String fuente, Supplier<T> consulta) {
		try {
			return consulta.get();
		} catch (RuntimeException e) {
			LOG.warn("Sin datos de {}: {}", fuente, e.toString());
			return null;
		}
	}
}
