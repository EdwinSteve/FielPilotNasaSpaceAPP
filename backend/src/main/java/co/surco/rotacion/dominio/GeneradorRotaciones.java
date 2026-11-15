package co.surco.rotacion.dominio;

import co.surco.rotacion.dominio.Candidatos.Cultivo;
import co.surco.rotacion.dominio.Rotacion.Temporada;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Algoritmo v1 de rotaciones (docs/decisiones/0008-recomendacion-y-rotacion.md). El modelo puntúa UNA
 * siembra; las secuencias de 2-3 temporadas se construyen encima:
 *
 * <ol>
 *   <li>Primera temporada: los {@value #PRIMEROS} cultivos aptos con mayor puntaje, en la fecha recomendada,
 *   con su P(éxito) del pronóstico (motor Bf).</li>
 *   <li>Temporadas siguientes: se siembra {@value #DESCANSO_DIAS} días después de la cosecha anterior. Esas
 *   fechas caen fuera de los 175 días del pronóstico, así que su P(éxito) es la frecuencia de éxito del
 *   cultivo en los años pasados del terreno para la fecha de la tabla histórica más cercana (cada 15 días).
 *   Si la siembra cae fuera de la tabla (más de 540 días), la secuencia termina ahí.</li>
 *   <li>Reglas: solo cultivos aptos (altitud y pH), ninguno repetido y nunca la misma familia botánica en
 *   dos temporadas seguidas (rompe ciclos de plagas y enfermedades).</li>
 *   <li>Puntaje = media de P(éxito) + {@value #BONO_LEGUMINOSA} si una leguminosa fijadora va antes de otro
 *   cultivo (le deja nitrógeno), recortado a 1. Se arman secuencias de 3 donde se puede y de 2 donde no.</li>
 *   <li>Top {@value #TOP}: de mayor a menor puntaje, sin repetir las dos primeras temporadas (para que las
 *   opciones sean distintas de verdad); si no alcanzan, se completan sin esa regla.</li>
 * </ol>
 *
 * Grado por puntaje: A+ >= 0,85; A >= 0,70; B >= 0,55; C >= 0,40; D por debajo.
 * Agua ahorrada: frente al monocultivo de referencia, que es lo que haría el agricultor sin rotar: sembrar
 * una y otra vez el cultivo comercial apto de mayor puntaje (un abono verde no cuenta: no se vende) en las
 * mismas fechas de la secuencia. Riego de la referencia menos riego de la secuencia (mm, año típico), por
 * el área del lote: 1 mm en 1 ha = 10 m³. Nunca negativa.
 */
public final class GeneradorRotaciones {

	public static final int PRIMEROS = 6;
	public static final int DESCANSO_DIAS = 15;
	public static final double BONO_LEGUMINOSA = 0.05;
	public static final int TOP = 3;
	static final int MAX_TEMPORADAS = 3;
	static final double M3_POR_MM_HA = 10;

	private GeneradorRotaciones() {
	}

	public static List<Rotacion> mejores(Candidatos c) {
		Map<String, Cultivo> porId = c.primera().stream().collect(Collectors.toMap(Cultivo::id, Function.identity(),
			(a, b) -> a));
		List<Cultivo> aptos = c.primera().stream().filter(Cultivo::apto).toList();
		List<Cultivo> primeros = aptos.stream().sorted(Comparator.comparingDouble(Cultivo::puntaje).reversed())
			.limit(PRIMEROS).toList();

		List<List<Temporada>> secuencias = new ArrayList<>();
		for (Cultivo a : primeros) {
			List<Temporada> inicio = List.of(new Temporada(a.id(), a.nombre(), a.familia(), a.siembra(), a.cosecha(),
				a.probExito(), a.riegoMm(), "pronostico"));
			extender(inicio, aptos, c.historico(), secuencias);
		}

		Cultivo referencia = aptos.stream().filter(x -> !x.abonoVerde())
			.max(Comparator.comparingDouble(Cultivo::puntaje)).orElse(primeros.isEmpty() ? null : primeros.getFirst());
		List<Rotacion> todas = secuencias.stream().map(s -> rotacion(s, porId, c, referencia)).sorted(Comparator
			.comparingDouble(Rotacion::puntaje).reversed().thenComparing(Rotacion::id)).toList();
		return elegir(todas);
	}

	/** Agrega temporadas mientras se pueda; guarda la secuencia cuando ya no cabe otra (mínimo 2). */
	static void extender(List<Temporada> seq, List<Cultivo> aptos, Candidatos.Historico h,
		List<List<Temporada>> salida) {
		if (seq.size() == MAX_TEMPORADAS) {
			salida.add(seq);
			return;
		}
		Temporada ultima = seq.getLast();
		LocalDate siembra = ultima.cosecha().plusDays(DESCANSO_DIAS);
		int i = indice(h, siembra);
		Set<String> usados = new HashSet<>();
		seq.forEach(t -> usados.add(t.cultivo()));
		boolean extendida = false;
		if (i >= 0) {
			for (Cultivo c : aptos) {
				Double p = valor(h.probExito(), c.id(), i);
				if (usados.contains(c.id()) || c.familia() == null || c.familia().equals(ultima.familia()) || p == null) {
					continue;
				}
				Double riego = valor(h.riegoMm(), c.id(), i);
				List<Temporada> nueva = new ArrayList<>(seq);
				nueva.add(new Temporada(c.id(), c.nombre(), c.familia(), siembra, siembra.plusDays(c.cicloDias()), p,
					riego == null ? 0 : riego, "historico"));
				extender(List.copyOf(nueva), aptos, h, salida);
				extendida = true;
			}
		}
		if (!extendida && seq.size() >= 2) {
			salida.add(seq);
		}
	}

	/** La fecha de la tabla histórica más cercana a {@code siembra}; -1 si cae fuera de la tabla. */
	static int indice(Candidatos.Historico h, LocalDate siembra) {
		if (h == null || h.fechas() == null || h.fechas().isEmpty()) {
			return -1;
		}
		List<LocalDate> f = h.fechas();
		if (siembra.isAfter(f.getLast().plusDays(7))) {
			return -1;
		}
		int mejor = 0;
		long menor = Long.MAX_VALUE;
		for (int i = 0; i < f.size(); i++) {
			long d = Math.abs(ChronoUnit.DAYS.between(f.get(i), siembra));
			if (d < menor) {
				menor = d;
				mejor = i;
			}
		}
		return mejor;
	}

	static Double valor(Map<String, List<Double>> tabla, String cultivo, int i) {
		List<Double> serie = tabla == null ? null : tabla.get(cultivo);
		return serie == null || i < 0 || i >= serie.size() ? null : serie.get(i);
	}

	static Rotacion rotacion(List<Temporada> seq, Map<String, Cultivo> porId, Candidatos c, Cultivo referencia) {
		double media = seq.stream().mapToDouble(Temporada::probExito).average().orElse(0);
		String aporte = aporteNitrogeno(seq, porId);
		double puntaje = Math.min(1, media + (aporte == null ? 0 : BONO_LEGUMINOSA));
		String id = seq.stream().map(Temporada::cultivo).collect(Collectors.joining("-"));
		return new Rotacion(id, grado(puntaje), seq, redondear(puntaje), redondear(media),
			(int) Math.round(media * 10), aguaAhorrada(seq, c, referencia), aporte);
	}

	/** La primera leguminosa que va antes de otro cultivo; null si no hay. */
	static String aporteNitrogeno(List<Temporada> seq, Map<String, Cultivo> porId) {
		for (int i = 0; i < seq.size() - 1; i++) {
			Cultivo leg = porId.get(seq.get(i).cultivo());
			if (leg != null && leg.fijaNitrogeno()) {
				String siguiente = seq.get(i + 1).nombre();
				return leg.fijacionKgHa() == null
					? "Antes de " + siguiente + " va " + leg.nombre() + ", que le deja nitrógeno al suelo."
					: "Antes de " + siguiente + " va " + leg.nombre() + ", que fija unos " + leg.fijacionKgHa()
						+ " kg de nitrógeno por hectárea.";
			}
		}
		return null;
	}

	/** Riego de repetir la referencia en las mismas fechas menos el de la secuencia, en m³ (>= 0). */
	static long aguaAhorrada(List<Temporada> seq, Candidatos c, Cultivo referencia) {
		if (referencia == null) {
			return 0;
		}
		double mono = referencia.riegoMm(); // primera temporada: con el pronóstico, en la misma fecha
		double secuencia = seq.getFirst().riegoMm();
		for (Temporada t : seq.subList(1, seq.size())) {
			Double r = valor(c.historico().riegoMm(), referencia.id(), indice(c.historico(), t.siembra()));
			mono += r == null ? t.riegoMm() : r;
			secuencia += t.riegoMm();
		}
		double m3 = Math.max(0, mono - secuencia) * c.areaHa() * M3_POR_MM_HA;
		return Math.round(m3 / 10) * 10;
	}

	public static String grado(double puntaje) {
		if (puntaje >= 0.85) {
			return "A+";
		}
		if (puntaje >= 0.70) {
			return "A";
		}
		if (puntaje >= 0.55) {
			return "B";
		}
		return puntaje >= 0.40 ? "C" : "D";
	}

	/** Las mejores {@value #TOP}, sin repetir las dos primeras temporadas; si no alcanzan, se completan. */
	static List<Rotacion> elegir(List<Rotacion> ordenadas) {
		List<Rotacion> elegidas = new ArrayList<>();
		Set<String> prefijos = new HashSet<>();
		for (Rotacion r : ordenadas) {
			if (elegidas.size() < TOP && prefijos.add(prefijo(r))) {
				elegidas.add(r);
			}
		}
		for (Rotacion r : ordenadas) {
			if (elegidas.size() < TOP && !elegidas.contains(r)) {
				elegidas.add(r);
			}
		}
		return List.copyOf(elegidas);
	}

	private static String prefijo(Rotacion r) {
		return r.temporadas().stream().limit(2).map(Temporada::cultivo).collect(Collectors.joining("-"));
	}

	private static double redondear(double x) {
		return Math.round(x * 1000) / 1000.0;
	}
}
