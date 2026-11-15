package co.surco.fincas.dominio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * El contorno del terreno: un polígono simple (sus lados no se cruzan) dibujado sobre el mapa.
 *
 * <p>El área y el centroide se calculan en una proyección local equirectangular centrada en el
 * terreno: para lotes de hasta decenas de kilómetros el error es menor al 0,1 %.
 */
public final class Poligono {

	public static final int MINIMO_VERTICES = 3;
	public static final int MAXIMO_VERTICES = 200;
	/** 100 m²: más pequeño es casi siempre un clic de más, no un terreno. */
	public static final double AREA_MINIMA_HA = 0.01;
	/** 20.000 ha: el límite de una finca muy grande en Colombia. */
	public static final double AREA_MAXIMA_HA = 20_000;

	private static final double RADIO_TIERRA_M = 6_371_008.8;
	private static final double TOLERANCIA_GRADOS = 1e-9;
	private static final Pattern NUMERO = Pattern.compile("-?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?");

	private final List<Coordenada> vertices;
	private final double areaHa;
	private final Coordenada centroide;

	private Poligono(List<Coordenada> vertices) {
		this.vertices = List.copyOf(vertices);
		double lat0 = vertices.stream().mapToDouble(Coordenada::lat).average().orElseThrow();
		double lon0 = vertices.stream().mapToDouble(Coordenada::lon).average().orElseThrow();
		double escalaX = Math.toRadians(1) * RADIO_TIERRA_M * Math.cos(Math.toRadians(lat0));
		double escalaY = Math.toRadians(1) * RADIO_TIERRA_M;
		int n = vertices.size();
		double doble = 0;
		double cx = 0;
		double cy = 0;
		for (int i = 0; i < n; i++) {
			Coordenada a = vertices.get(i);
			Coordenada b = vertices.get((i + 1) % n);
			double xa = (a.lon() - lon0) * escalaX;
			double ya = (a.lat() - lat0) * escalaY;
			double xb = (b.lon() - lon0) * escalaX;
			double yb = (b.lat() - lat0) * escalaY;
			double cruz = xa * yb - xb * ya;
			doble += cruz;
			cx += (xa + xb) * cruz;
			cy += (ya + yb) * cruz;
		}
		this.areaHa = Math.abs(doble) / 2 / 10_000;
		this.centroide = doble == 0 ? new Coordenada(lat0, lon0)
			: new Coordenada(lat0 + cy / (3 * doble) / escalaY, lon0 + cx / (3 * doble) / escalaX);
	}

	/**
	 * Valida y crea el polígono. Acepta el anillo abierto o cerrado (el último punto igual al primero)
	 * y quita los puntos repetidos seguidos (un doble clic en el mapa).
	 */
	public static Poligono de(List<Coordenada> puntos) {
		if (puntos == null) {
			throw ErroresFincas.muyPocosVertices();
		}
		List<Coordenada> limpios = new ArrayList<>();
		for (Coordenada c : puntos) {
			if (limpios.isEmpty() || !iguales(limpios.getLast(), c)) {
				limpios.add(c);
			}
		}
		if (limpios.size() > 1 && iguales(limpios.getFirst(), limpios.getLast())) {
			limpios.removeLast();
		}
		if (limpios.size() < MINIMO_VERTICES) {
			throw ErroresFincas.muyPocosVertices();
		}
		if (limpios.size() > MAXIMO_VERTICES) {
			throw ErroresFincas.demasiadosVertices();
		}
		if (!limpios.stream().allMatch(Coordenada::enColombia)) {
			throw ErroresFincas.fueraDeColombia();
		}
		if (seCruza(limpios)) {
			throw ErroresFincas.ladosCruzados();
		}
		Poligono p = new Poligono(limpios);
		if (p.areaHa < AREA_MINIMA_HA) {
			throw ErroresFincas.areaMuyPequena();
		}
		if (p.areaHa > AREA_MAXIMA_HA) {
			throw ErroresFincas.areaMuyGrande();
		}
		return p;
	}

	/** Lee el GeoJSON que escribe {@link #aGeoJson()}. Solo para datos ya validados (los de la base). */
	public static Poligono desdeGeoJson(String geojson) {
		Matcher m = NUMERO.matcher(geojson.substring(geojson.indexOf("coordinates")));
		List<Double> numeros = new ArrayList<>();
		while (m.find()) {
			numeros.add(Double.parseDouble(m.group()));
		}
		List<Coordenada> puntos = new ArrayList<>();
		for (int i = 0; i + 1 < numeros.size(); i += 2) {
			puntos.add(new Coordenada(numeros.get(i + 1), numeros.get(i))); // GeoJSON: [lon, lat]
		}
		return de(puntos);
	}

	/** GeoJSON (RFC 7946): {@code {"type":"Polygon","coordinates":[[[lon,lat],...,[lon0,lat0]]]}}. */
	public String aGeoJson() {
		StringBuilder sb = new StringBuilder("{\"type\":\"Polygon\",\"coordinates\":[[");
		for (int i = 0; i <= vertices.size(); i++) {
			Coordenada c = vertices.get(i % vertices.size());
			if (i > 0) {
				sb.append(',');
			}
			sb.append(String.format(Locale.ROOT, "[%.7f,%.7f]", c.lon(), c.lat()));
		}
		return sb.append("]]}").toString();
	}

	public List<Coordenada> vertices() {
		return vertices;
	}

	public double areaHa() {
		return areaHa;
	}

	public Coordenada centroide() {
		return centroide;
	}

	private static boolean iguales(Coordenada a, Coordenada b) {
		return Math.abs(a.lat() - b.lat()) < TOLERANCIA_GRADOS && Math.abs(a.lon() - b.lon()) < TOLERANCIA_GRADOS;
	}

	/** ¿Algún par de lados no vecinos se toca? O(n²), con n ≤ 200 es instantáneo. */
	private static boolean seCruza(List<Coordenada> v) {
		int n = v.size();
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				boolean vecinos = j == i + 1 || (i == 0 && j == n - 1);
				if (!vecinos && segmentosSeTocan(v.get(i), v.get((i + 1) % n), v.get(j), v.get((j + 1) % n))) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean segmentosSeTocan(Coordenada a, Coordenada b, Coordenada c, Coordenada d) {
		double d1 = orientacion(c, d, a);
		double d2 = orientacion(c, d, b);
		double d3 = orientacion(a, b, c);
		double d4 = orientacion(a, b, d);
		if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
			return true;
		}
		return (d1 == 0 && sobre(c, d, a)) || (d2 == 0 && sobre(c, d, b))
			|| (d3 == 0 && sobre(a, b, c)) || (d4 == 0 && sobre(a, b, d));
	}

	private static double orientacion(Coordenada p, Coordenada q, Coordenada r) {
		return (q.lon() - p.lon()) * (r.lat() - p.lat()) - (q.lat() - p.lat()) * (r.lon() - p.lon());
	}

	private static boolean sobre(Coordenada p, Coordenada q, Coordenada r) {
		return Math.min(p.lon(), q.lon()) <= r.lon() && r.lon() <= Math.max(p.lon(), q.lon())
			&& Math.min(p.lat(), q.lat()) <= r.lat() && r.lat() <= Math.max(p.lat(), q.lat());
	}
}
