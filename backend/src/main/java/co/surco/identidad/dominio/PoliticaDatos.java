package co.surco.identidad.dominio;

/** Política de tratamiento de datos (Ley 1581 de 2012) que el productor acepta al registrarse. */
public record PoliticaDatos(String version, String urlTexto, String contenido) {
}
