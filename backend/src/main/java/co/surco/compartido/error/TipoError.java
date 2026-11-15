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
	/** Bien formada, pero no se puede atender con los datos que hay (terreno sin suelo en SoilGrids): 422. */
	NO_PROCESABLE,
	/** Cuenta bloqueada temporalmente: 423. */
	BLOQUEADO,
	/**
	 * Una fuente externa de la que dependemos (NASA POWER, SoilGrids, IDEAM) respondió mal, no respondió o envió
	 * algo que no entendemos (un nivel de alerta nuevo del IDEAM): 502.
	 */
	FUENTE_EXTERNA,
	/** Un servicio del que dependemos (el modelo de recomendación) no responde: 503. */
	NO_DISPONIBLE
}
