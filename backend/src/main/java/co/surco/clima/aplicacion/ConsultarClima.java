package co.surco.clima.aplicacion;

import co.surco.clima.dominio.Avisos;
import co.surco.clima.dominio.ErroresClima;
import co.surco.clima.dominio.ErroresClima.ClimaNoDisponible;
import co.surco.clima.dominio.Puertos.DatosSatelite;
import co.surco.clima.dominio.Puertos.FuenteAlertasRios;
import co.surco.clima.dominio.Puertos.FuenteSatelital;
import co.surco.clima.dominio.Puertos.FuenteTiempo;
import co.surco.clima.dominio.Puertos.Tiempo;
import co.surco.clima.dominio.Puertos.ValorDiario;
import co.surco.clima.dominio.ResumenClima;
import co.surco.clima.dominio.ResumenClima.Aviso;
import co.surco.clima.dominio.ResumenClima.Satelite;
import co.surco.clima.dominio.ResumenClima.Ubicacion;
import co.surco.territorio.ConsultaTerritorio;
import co.surco.territorio.Departamento;
import co.surco.territorio.Municipio;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Arma el resumen del clima de un punto. Las tres fuentes se consultan al mismo tiempo; solo el
 * pronóstico es obligatorio. Cada respuesta se guarda 10 minutos para no consultar en cada recarga.
 */
@Service
public class ConsultarClima {

	static final String OPEN_METEO = "Open-Meteo";
	static final String NASA_POWER = "NASA POWER";
	static final String IDEAM = "IDEAM";

	private static final Logger LOG = LoggerFactory.getLogger(ConsultarClima.class);
	private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");
	private static final Duration VIGENCIA = Duration.ofMinutes(10);
	private static final int MAXIMO_EN_CACHE = 500;

	private final ConsultaTerritorio territorio;
	private final FuenteTiempo fuenteTiempo;
	private final FuenteSatelital fuenteSatelital;
	private final FuenteAlertasRios fuenteAlertas;
	private final Clock reloj;
	private final Map<String, EnCache> cache = new ConcurrentHashMap<>();

	private record EnCache(Instant momento, ResumenClima resumen) {
	}

	public ConsultarClima(ConsultaTerritorio territorio, FuenteTiempo fuenteTiempo, FuenteSatelital fuenteSatelital,
		FuenteAlertasRios fuenteAlertas, Clock reloj) {
		this.territorio = territorio;
		this.fuenteTiempo = fuenteTiempo;
		this.fuenteSatelital = fuenteSatelital;
		this.fuenteAlertas = fuenteAlertas;
		this.reloj = reloj;
	}

	/**
	 * @param codigoMunicipio código DIVIPOLA; da el nombre del lugar y, si no llegan coordenadas, el punto
	 *                        (la cabecera municipal)
	 * @param lat             latitud del terreno, opcional
	 * @param lon             longitud del terreno, opcional
	 */
	public ResumenClima resumen(String codigoMunicipio, Double lat, Double lon) {
		Municipio municipio = null;
		if (codigoMunicipio != null && !codigoMunicipio.isBlank()) {
			municipio = territorio.municipio(codigoMunicipio.trim()).orElseThrow(ErroresClima::municipioNoEncontrado);
		}
		boolean conPunto = lat != null && lon != null;
		if (!conPunto && (municipio == null || municipio.lat() == null || municipio.lon() == null)) {
			throw ErroresClima.ubicacionRequerida();
		}
		double latitud = conPunto ? lat : municipio.lat();
		double longitud = conPunto ? lon : municipio.lon();
		if (latitud < -4.5 || latitud > 13.5 || longitud < -82 || longitud > -66) {
			throw ErroresClima.fueraDeColombia();
		}

		String clave = String.format(Locale.ROOT, "%s|%.2f,%.2f", municipio == null ? "" : municipio.codigo(), latitud,
			longitud);
		Instant ahora = reloj.instant();
		EnCache guardado = cache.get(clave);
		if (guardado != null && guardado.momento().plus(VIGENCIA).isAfter(ahora)) {
			return guardado.resumen();
		}
		ResumenClima resumen = consultar(municipio, latitud, longitud, ahora);
		if (cache.size() >= MAXIMO_EN_CACHE) {
			cache.clear();
		}
		cache.put(clave, new EnCache(ahora, resumen));
		return resumen;
	}

	private ResumenClima consultar(Municipio municipio, double lat, double lon, Instant ahora) {
		CompletableFuture<Tiempo> tiempoFuturo = CompletableFuture.supplyAsync(() -> fuenteTiempo.consultar(lat, lon));
		CompletableFuture<Optional<DatosSatelite>> sateliteFuturo = CompletableFuture
			.supplyAsync(() -> opcional(NASA_POWER, () -> fuenteSatelital.consultar(lat, lon)));
		// Optional de Optional: por fuera, si el IDEAM respondió; por dentro, si hay alerta en el punto
		CompletableFuture<Optional<Optional<Aviso>>> riosFuturo = CompletableFuture
			.supplyAsync(() -> opcional(IDEAM, () -> fuenteAlertas.vigenteEn(lat, lon).flatMap(Avisos::deRios)));

		Tiempo tiempo;
		try {
			tiempo = tiempoFuturo.join();
		} catch (RuntimeException e) {
			LOG.error("No se pudo consultar el pronóstico de ({}, {}): {}", lat, lon, e.toString());
			throw new ClimaNoDisponible(e);
		}
		Optional<DatosSatelite> satelite = sateliteFuturo.join();
		Optional<Optional<Aviso>> rios = riosFuturo.join();

		List<String> fuentes = new ArrayList<>(List.of(OPEN_METEO));
		List<Aviso> avisos = new ArrayList<>();
		Avisos.deLluviaFuerte(tiempo.dias(), OPEN_METEO).ifPresent(avisos::add);
		satelite.ifPresent(s -> {
			fuentes.add(NASA_POWER);
			LocalDate hoy = ahora.atZone(COLOMBIA).toLocalDate();
			Avisos.deSequia(s.lluviaDiaria(), s.lluviaNormalMes(), hoy, NASA_POWER).ifPresent(avisos::add);
		});
		rios.ifPresent(aviso -> {
			fuentes.add(IDEAM);
			aviso.ifPresent(avisos::add);
		});

		return new ResumenClima(
			new Ubicacion(municipio == null ? null : municipio.nombre(), departamentoDe(municipio), lat, lon,
				tiempo.altitudM()),
			ahora.atZone(COLOMBIA).toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS), tiempo.actual(),
			satelite.map(ConsultarClima::aSatelite).orElse(null), tiempo.dias(), Avisos.ordenar(avisos),
			List.copyOf(fuentes));
	}

	/** Una fuente que no es obligatoria: si falla, se registra y el resumen sale sin esa parte. */
	private static <T> Optional<T> opcional(String fuente, Supplier<T> consulta) {
		try {
			return Optional.of(consulta.get());
		} catch (RuntimeException e) {
			LOG.warn("{} no respondió: {}", fuente, e.toString());
			return Optional.empty();
		}
	}

	private static Satelite aSatelite(DatosSatelite datos) {
		ValorDiario radiacion = datos.radiacionMj();
		ValorDiario humedad = datos.humedadRaices();
		return new Satelite(
			radiacion == null ? null : redondear(radiacion.valor() / 3.6), // MJ/m² a kWh/m²
			radiacion == null ? null : radiacion.fecha(),
			humedad == null ? null : redondear(humedad.valor() * 100),
			humedad == null ? null : humedad.fecha());
	}

	private String departamentoDe(Municipio municipio) {
		if (municipio == null) {
			return null;
		}
		return territorio.departamentos().stream()
			.filter(d -> d.codigo().equals(municipio.codigoDepartamento()))
			.map(Departamento::nombre).findFirst().orElse(null);
	}

	private static double redondear(double valor) {
		return Math.round(valor * 10) / 10.0;
	}
}
