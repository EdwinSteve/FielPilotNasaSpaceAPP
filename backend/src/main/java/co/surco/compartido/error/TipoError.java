package co.surco.compartido.error;

/**
 * Clase de error de negocio. El dominio dice QUÉ pasó; el manejador web decide el código HTTP.
 * Así el dominio no depende de Spring ni de HTTP.
 */
public enum TipoError {
	/** Datos que no cumplen una regla: 400. */
	SOLICITUD_INVALIDA,
	/** Sin credenciales válidas: 401. */
	NO_AUTENTICADO,
	/** Autenticado pero sin permiso: 403. */
	PROHIBIDO,
	/** El recurso no existe o no es del usuario: 404. */
	NO_ENCONTRADO,
	/** Choca con el estado actual (duplicado, versión vieja): 409. */
	CONFLICTO,
	/** Cuenta bloqueada temporalmente: 423. */
	BLOQUEADO
}
