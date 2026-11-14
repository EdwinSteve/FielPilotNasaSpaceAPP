// Tipos del módulo identidad: reflejan los schemas del backend (/api/v1/auth y /politicas).
import type { Tokens } from "@/compartido/api/tipos";

export interface Usuario {
  id: string;
  nombre_completo: string;
  celular: string;
  correo: string | null;
  municipio_residencia: string;
  departamento_residencia: string;
  rol: "PRODUCTOR" | "ADMIN";
  creado_en: string;
}

export interface PoliticaVigente {
  version: string;
  url_texto: string;
  contenido: string;
}

export interface DatosIngreso {
  celular: string;
  contrasena: string;
}

export interface DatosRegistro {
  nombre_completo: string;
  celular: string;
  correo: string;
  contrasena: string;
  /** Solo para el formulario: el backend deduce el departamento del código del municipio. */
  departamento_residencia: string;
  municipio_residencia: string;
  acepta_politica: boolean;
}

export interface RespuestaRegistro {
  usuario: Usuario;
  tokens: Tokens;
}

/** Errores de un formulario, por nombre de campo. */
export type ErroresFormulario<C extends string> = Partial<Record<C, string>>;

/** Códigos de error del backend que la UI trata de forma especial. */
export const CODIGOS = {
  credencialesInvalidas: "AUTH_CREDENCIALES_INVALIDAS",
  cuentaBloqueada: "AUTH_CUENTA_BLOQUEADA",
  celularDuplicado: "USUARIO_CELULAR_DUPLICADO",
  correoDuplicado: "USUARIO_CORREO_DUPLICADO",
  consentimientoRequerido: "CONSENTIMIENTO_REQUERIDO",
  politicaDesactualizada: "POLITICA_DESACTUALIZADA",
} as const;
