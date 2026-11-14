package co.surco.identidad.dominio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Genera refresh tokens opacos (256 bits aleatorios) y calcula el hash que se guarda en la base. */
public final class TokensRefresco {

	private static final SecureRandom ALEATORIO = new SecureRandom();

	private TokensRefresco() {
	}

	/** 32 bytes aleatorios en Base64 URL-safe: 43 caracteres. */
	public static String generar() {
		byte[] bytes = new byte[32];
		ALEATORIO.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** SHA-256 en hexadecimal. Basta un hash rápido: el token ya tiene 256 bits de azar. */
	public static String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 no disponible", e);
		}
	}
}
