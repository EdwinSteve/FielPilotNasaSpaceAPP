package co.surco.alertas.aplicacion;

import co.surco.alertas.dominio.ErroresAlertas;
import co.surco.alertas.dominio.IndicadorSequia;
import co.surco.alertas.dominio.NivelRios;
import co.surco.alertas.dominio.NivelSequia;
import co.surco.alertas.dominio.Puertos.FuenteAlertasRios;
import co.surco.alertas.dominio.Puertos.FuenteLluvia;
import co.surco.alertas.dominio.Puertos.Lluvia;
import co.surco.alertas.dominio.Puertos.SubzonaConAlerta;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Alertas de un punto: ríos y quebradas (IDEAM) e indicador de sequía (NASA POWER).
 * Sin transacción: no escribe en la base.
 */
@Service
public class ConsultarAlertas {

	private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

	private final FuenteAlertasRios ideam;
	private final FuenteLluvia lluvia;

	public ConsultarAlertas(FuenteAlertasRios ideam, FuenteLluvia lluvia) {
		this.ideam = ideam;
		this.lluvia = lluvia;
	}

	/** {@code subzona} es null cuando el nivel es SIN_ALERTA. */
	public record AlertaRios(NivelRios nivel, String subzona, double lat, double lon, OffsetDateTime consultadoEn) {
	}

	public record Sequia(NivelSequia nivel, double lat, double lon, OffsetDateTime consultadoEn) {
	}

	public AlertaRios rios(double lat, double lon) {
		validarCuadroDeColombia(lat, lon);
		Optional<SubzonaConAlerta> conAlerta = ideam.conAlerta(lat, lon);
		if (conAlerta.isEmpty()) {
			// La capa de alertas solo trae subzonas con alerta vigente: no aparecer = sin alerta,
			// siempre que el punto esté en Colombia (el cuadro de arriba incluye algo de los países vecinos).
			if (!ideam.dentroDeColombia(lat, lon)) {
				throw ErroresAlertas.fueraDeColombia();
			}
			return new AlertaRios(NivelRios.SIN_ALERTA, null, lat, lon, ahora());
		}
		SubzonaConAlerta subzona = conAlerta.get();
		return new AlertaRios(NivelRios.desdeIdeam(subzona.nivelIdeam()), subzona.nombre(), lat, lon, ahora());
	}

	public Sequia sequia(double lat, double lon) {
		validarCuadroDeColombia(lat, lon);
		Lluvia datos = lluvia.lluviaEn(lat, lon);
		IndicadorSequia indicador = IndicadorSequia.calcular(datos.diaria(), datos.normalPorMes());
		return new Sequia(indicador.nivel(), lat, lon, ahora());
	}

	/** Descarta rápido lo que está lejos, sin llamar a nadie. */
	private static void validarCuadroDeColombia(double lat, double lon) {
		boolean dentro = lat >= -4.5 && lat <= 13.5 && lon >= -82 && lon <= -66; // también rechaza NaN
		if (!dentro) {
			throw ErroresAlertas.fueraDeColombia();
		}
	}

	private static OffsetDateTime ahora() {
		return OffsetDateTime.now(COLOMBIA).truncatedTo(ChronoUnit.SECONDS);
	}
}
