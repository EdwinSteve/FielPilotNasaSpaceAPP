package co.surco.compartido.error;

import java.util.List;

/**
 * Error de negocio. Todos los módulos lanzan esta clase (o una subclase) y el
 * {@code ManejadorErrores} la convierte en {@code {"code", "message", "details"?}}.
 *
 * <p>El mensaje va de usted y en palabras sencillas: lo lee el agricultor.
 */
public class ErrorDominio extends RuntimeException {

	private final String codigo;
	private final TipoError tipo;
	private final transient List<ErrorCampo> detalles;

	public ErrorDominio(String codigo, String mensaje, TipoError tipo) {
		this(codigo, mensaje, tipo, List.of());
	}

	public ErrorDominio(String codigo, String mensaje, TipoError tipo, List<ErrorCampo> detalles) {
		super(mensaje);
		this.codigo = codigo;
		this.tipo = tipo;
		this.detalles = List.copyOf(detalles);
	}

	/** Error de validación de un solo campo: el frontend lo muestra junto a ese campo. */
	public static ErrorDominio deCampo(String codigo, String campo, String mensaje) {
		return new ErrorDominio(codigo, mensaje, TipoError.SOLICITUD_INVALIDA, List.of(new ErrorCampo(campo, mensaje)));
	}

	public String codigo() {
		return codigo;
	}

	public TipoError tipo() {
		return tipo;
	}

	public List<ErrorCampo> detalles() {
		return detalles;
	}
}
