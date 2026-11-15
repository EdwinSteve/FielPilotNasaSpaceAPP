package co.surco.clima.dominio;

import co.surco.clima.dominio.Puertos.ClimaAhora;
import co.surco.clima.dominio.Puertos.DatoDiario;
import co.surco.clima.dominio.Puertos.SueloYSol;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

/** Arma las seis tarjetas, siempre en el mismo orden, a partir de lo que hayan respondido las fuentes. */
public final class Tarjetas {

	public static final String GOOGLE = "Google Weather";
	public static final String NASA = "NASA POWER";
	/** Decimales con coma en los textos. */
	private static final Locale ES = Locale.forLanguageTag("es-CO");
	private static final String[] RUMBOS = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};

	private Tarjetas() {
	}

	/** {@code ahora} y {@code sueloYSol} son null si su fuente falló: sus tarjetas salen sin dato. */
	public static List<Condicion> armar(ClimaAhora ahora, SueloYSol sueloYSol) {
		DatoDiario radiacion = sueloYSol == null ? null : sueloYSol.radiacionMj();
		DatoDiario suelo = sueloYSol == null ? null : sueloYSol.humedadSuelo();
		return List.of(temperatura(ahora), lluvia(ahora), humedad(ahora), viento(ahora), radiacion(radiacion, ahora),
			suelo(suelo));
	}

	private static Condicion temperatura(ClimaAhora g) {
		if (g == null || g.temperaturaC() == null) {
			return Condicion.sinDato("TEMPERATURA_AIRE", "°C", GOOGLE);
		}
		String detalle = g.cambio24hC() == null ? "" : String.format(ES, "%+.1f °C vs. ayer a esta hora", g.cambio24hC());
		return new Condicion("TEMPERATURA_AIRE", redondear(g.temperaturaC()), "°C", detalle, GOOGLE, g.medido());
	}

	private static Condicion lluvia(ClimaAhora g) {
		if (g == null || g.lluvia24hMm() == null) {
			return Condicion.sinDato("PRECIPITACION_24H", "mm", GOOGLE);
		}
		StringJoiner detalle = new StringJoiner(" · ");
		if (g.lluviaUltimaHoraMm() != null) {
			detalle.add(String.format(ES, "Última hora: %.1f mm", g.lluviaUltimaHoraMm()));
		}
		if (g.probabilidadLluvia() != null) {
			detalle.add("probabilidad de lluvia " + g.probabilidadLluvia() + " %");
		}
		return new Condicion("PRECIPITACION_24H", redondear(g.lluvia24hMm()), "mm", detalle.toString(), GOOGLE,
			g.medido());
	}

	private static Condicion humedad(ClimaAhora g) {
		if (g == null || g.humedadRelativa() == null) {
			return Condicion.sinDato("HUMEDAD_RELATIVA", "%", GOOGLE);
		}
		String detalle = g.puntoRocioC() == null ? "" : String.format(ES, "Punto de rocío: %.1f °C", g.puntoRocioC());
		return new Condicion("HUMEDAD_RELATIVA", g.humedadRelativa().doubleValue(), "%", detalle, GOOGLE, g.medido());
	}

	private static Condicion viento(ClimaAhora g) {
		if (g == null || g.vientoKmh() == null) {
			return Condicion.sinDato("VIENTO", "km/h", GOOGLE);
		}
		StringJoiner detalle = new StringJoiner(" · ");
		if (g.direccionVientoGrados() != null) {
			detalle.add("Dirección " + rumbo(g.direccionVientoGrados()));
		}
		if (g.rafagasKmh() != null) {
			detalle.add(String.format(ES, "ráfagas %.0f km/h", g.rafagasKmh()));
		}
		return new Condicion("VIENTO", redondear(g.vientoKmh()), "km/h", detalle.toString(), GOOGLE, g.medido());
	}

	/** De MJ/m² a kWh/m² (÷ 3,6). El índice UV, si lo hay, es de Google. */
	private static Condicion radiacion(DatoDiario dato, ClimaAhora g) {
		if (dato == null) {
			return Condicion.sinDato("RADIACION_SOLAR", "kWh/m²", NASA);
		}
		StringJoiner detalle = new StringJoiner(" · ");
		detalle.add("Total del día");
		if (g != null && g.indiceUv() != null) {
			detalle.add("índice UV " + g.indiceUv());
		}
		return new Condicion("RADIACION_SOLAR", redondear(dato.valor() / 3.6), "kWh/m²", detalle.toString(), NASA,
			dato.fecha().toString());
	}

	/** De fracción (0 a 1) a porcentaje. */
	private static Condicion suelo(DatoDiario dato) {
		if (dato == null) {
			return Condicion.sinDato("HUMEDAD_SUELO", "%", NASA);
		}
		return new Condicion("HUMEDAD_SUELO", redondear(dato.valor() * 100), "%", "Humedad en la zona de raíces", NASA,
			dato.fecha().toString());
	}

	/** Grados (0 = norte, en sentido del reloj) a uno de los 8 rumbos. */
	public static String rumbo(double grados) {
		return RUMBOS[(int) Math.round(((grados % 360) + 360) % 360 / 45.0) % 8];
	}

	private static double redondear(double valor) {
		return Math.round(valor * 10) / 10.0;
	}
}
