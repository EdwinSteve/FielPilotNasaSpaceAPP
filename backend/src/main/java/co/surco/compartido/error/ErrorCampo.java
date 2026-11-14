package co.surco.compartido.error;

/** Error de un campo del formulario. {@code field} usa el nombre del JSON (snake_case). */
public record ErrorCampo(String field, String message) {
}
