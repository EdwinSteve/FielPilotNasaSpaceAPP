// Tipos comunes a todos los módulos: errores y tokens de sesión.

export interface ErrorDetalle {
  campo: string;
  mensaje: string;
}

/** Error que ve la UI. El backend responde { code, message, details? } y http.ts lo traduce. */
export interface ErrorApi {
  codigo: string;
  mensaje: string;
  detalles?: ErrorDetalle[];
}

export interface Tokens {
  access_token: string;
  refresh_token: string;
  token_type: "bearer";
  expira_en: number;
}

export function esErrorApi(valor: unknown): valor is ErrorApi {
  return typeof valor === "object" && valor !== null && "codigo" in valor && "mensaje" in valor;
}

/** Convierte cualquier excepción en un ErrorApi mostrable. */
export function aErrorApi(valor: unknown): ErrorApi {
  if (esErrorApi(valor)) return valor;
  return { codigo: "ERROR_DESCONOCIDO", mensaje: "Ocurrió un error inesperado. Inténtelo de nuevo." };
}
