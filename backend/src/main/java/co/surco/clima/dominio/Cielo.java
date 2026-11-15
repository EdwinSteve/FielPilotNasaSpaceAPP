package co.surco.clima.dominio;

/** Traduce el código de tiempo de la OMM (el que entrega Open-Meteo) a palabras y a un icono. */
public final class Cielo {

	/** Desde esta lluvia en un día se dibuja el icono de lluvia. */
	private static final double LLUVIA_VISIBLE_MM = 1.0;

	private Cielo() {
	}

	public static String descripcion(int codigo) {
		return switch (codigo) {
			case 0 -> "Despejado";
			case 1 -> "Mayormente despejado";
			case 2 -> "Parcialmente nublado";
			case 3 -> "Nublado";
			case 45, 48 -> "Niebla";
			case 51, 53, 55, 56, 57 -> "Llovizna";
			case 61, 66 -> "Lluvia ligera";
			case 63 -> "Lluvia moderada";
			case 65, 67 -> "Lluvia fuerte";
			case 80 -> "Chubascos ligeros";
			case 81 -> "Chubascos";
			case 82 -> "Chubascos fuertes";
			case 71, 73, 75, 77, 85, 86 -> "Nieve";
			case 95, 96, 99 -> "Tormenta eléctrica";
			default -> "Variable";
		};
	}

	/** Icono de lo que pasa ahora mismo. */
	public static String icono(int codigo) {
		if (codigo >= 51) {
			return "rain";
		}
		return codigo <= 1 ? "sun" : "cloudsun";
	}

	/** Icono de un día completo: manda la lluvia esperada, no una llovizna de minutos. */
	public static String iconoDelDia(int codigo, double lluviaMm) {
		if (lluviaMm >= LLUVIA_VISIBLE_MM) {
			return "rain";
		}
		return codigo <= 1 ? "sun" : "cloudsun";
	}

	/** Grados (0 = norte, en el sentido del reloj) a uno de los 8 rumbos. */
	public static String rumbo(double grados) {
		String[] rumbos = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
		return rumbos[(int) Math.round(((grados % 360) + 360) % 360 / 45.0) % 8];
	}
}
