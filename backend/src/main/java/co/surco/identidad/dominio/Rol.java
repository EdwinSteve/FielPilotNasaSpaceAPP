package co.surco.identidad.dominio;

/** Rol del usuario. Viaja en el claim {@code rol} del JWT y se vuelve la autoridad ROLE_&lt;rol&gt;. */
public enum Rol {
	/** Agricultor: el rol de toda cuenta nueva. */
	PRODUCTOR,
	/** Equipo de SURCO: puede administrar catálogos y ver datos de otros. Se asigna a mano en la base. */
	ADMIN
}
