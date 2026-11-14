// Llamadas HTTP del módulo identidad contra la API de Spring Boot (backend/, módulo identidad).
// Usan el cliente de src/compartido/api/http.ts, que agrega el token y lo renueva si vence.
import { peticion } from "@/compartido/api/http";
import { cerrarSesionLocal, guardarSesion, obtenerTokens } from "@/compartido/api/sesion";
import type { Tokens } from "@/compartido/api/tipos";
import type { DatosIngreso, DatosRegistro, PoliticaVigente, RespuestaRegistro, Usuario } from "./tipos";

/** GET /politicas/vigente: versión y texto de la política que se acepta al registrarse. Pública. */
export async function obtenerPoliticaVigente(): Promise<PoliticaVigente> {
  return peticion<PoliticaVigente>("GET", "/politicas/vigente");
}

/** POST /auth/login: inicia sesión con celular y contraseña y guarda los tokens. */
export async function iniciarSesion(datos: DatosIngreso): Promise<Tokens> {
  const tokens = await peticion<Tokens>("POST", "/auth/login", { cuerpo: datos });
  guardarSesion(tokens);
  return tokens;
}

/**
 * POST /auth/registro: crea la cuenta y deja la sesión iniciada.
 * Los nombres del JSON son los mismos del formulario, así los errores por campo llegan directo a su campo.
 * El departamento no se envía: el backend lo deduce de los dos primeros dígitos del municipio.
 */
export async function registrarUsuario(datos: DatosRegistro, versionPolitica: string): Promise<RespuestaRegistro> {
  const respuesta = await peticion<RespuestaRegistro>("POST", "/auth/registro", {
    cuerpo: {
      nombre_completo: datos.nombre_completo.trim(),
      celular: datos.celular,
      correo: datos.correo.trim() || null,
      contrasena: datos.contrasena,
      municipio_residencia: datos.municipio_residencia,
      acepta_politica: datos.acepta_politica,
      version_politica: versionPolitica,
    },
  });
  guardarSesion(respuesta.tokens);
  return respuesta;
}

/** GET /usuarios/me: perfil del usuario de la sesión. Ruta protegida (exige el access token). */
export async function obtenerMiPerfil(): Promise<Usuario> {
  return peticion<Usuario>("GET", "/usuarios/me", { autenticada: true });
}

/**
 * POST /auth/logout: revoca el refresh token de esta sesión. Es pública en el backend para que se pueda
 * salir aunque el access token ya haya vencido. La sesión local se cierra aunque la llamada falle.
 */
export async function cerrarSesion(): Promise<void> {
  const tokens = obtenerTokens();
  try {
    if (tokens) {
      await peticion<void>("POST", "/auth/logout", { cuerpo: { refresh_token: tokens.refresh_token } });
    }
  } finally {
    cerrarSesionLocal();
  }
}
