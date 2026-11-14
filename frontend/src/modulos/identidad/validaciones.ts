// Validaciones de los formularios de identidad. Funciones puras: mismas reglas que el backend,
// para que el agricultor vea el error antes de enviar.
import type { DatosIngreso, DatosRegistro, ErroresFormulario } from "./tipos";

export const MENSAJES = {
  celular: "El celular debe tener 10 dígitos y empezar por el número 3.",
  contrasenaVacia: "Escriba su contraseña.",
  contrasenaDebil: "La contraseña debe tener mínimo 8 caracteres, al menos una letra y un número.",
  nombre: "Escriba su nombre y apellido completos (mínimo 3 letras).",
  correo: "Escriba un correo electrónico válido o deje este campo vacío.",
  departamento: "Elija el departamento donde vive.",
  municipio: "Elija el municipio donde vive.",
  politica: "Debe aceptar la política de datos para continuar.",
} as const;

export function celularValido(celular: string): boolean {
  return /^3\d{9}$/.test(celular);
}

export function contrasenaSegura(contrasena: string): boolean {
  return contrasena.length >= 8 && contrasena.length <= 128 && /[a-zA-Z]/.test(contrasena) && /\d/.test(contrasena);
}

export function correoValido(correo: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correo);
}

export function validarIngreso(datos: DatosIngreso): ErroresFormulario<keyof DatosIngreso> {
  const errores: ErroresFormulario<keyof DatosIngreso> = {};
  if (!celularValido(datos.celular)) errores.celular = MENSAJES.celular;
  if (!datos.contrasena) errores.contrasena = MENSAJES.contrasenaVacia;
  return errores;
}

export function validarRegistro(datos: DatosRegistro): ErroresFormulario<keyof DatosRegistro> {
  const errores: ErroresFormulario<keyof DatosRegistro> = {};
  const nombre = datos.nombre_completo.trim();
  if (nombre.length < 3 || nombre.length > 120) errores.nombre_completo = MENSAJES.nombre;
  if (!celularValido(datos.celular)) errores.celular = MENSAJES.celular;
  const correo = datos.correo.trim();
  if (correo && !correoValido(correo)) errores.correo = MENSAJES.correo;
  if (!contrasenaSegura(datos.contrasena)) errores.contrasena = MENSAJES.contrasenaDebil;
  if (!datos.departamento_residencia) errores.departamento_residencia = MENSAJES.departamento;
  else if (!datos.municipio_residencia) errores.municipio_residencia = MENSAJES.municipio;
  if (!datos.acepta_politica) errores.acepta_politica = MENSAJES.politica;
  return errores;
}

export function sinErrores(errores: object): boolean {
  return Object.keys(errores).length === 0;
}
