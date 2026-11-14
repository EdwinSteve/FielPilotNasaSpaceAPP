package co.surco.identidad.aplicacion;

/**
 * Lo que recibe el cliente al iniciar o renovar la sesión.
 *
 * @param accessToken       JWT para el encabezado {@code Authorization: Bearer}
 * @param refreshToken      token opaco para pedir un access token nuevo (un solo uso)
 * @param expiraEnSegundos  vida del access token
 */
public record Tokens(String accessToken, String refreshToken, long expiraEnSegundos) {
}
