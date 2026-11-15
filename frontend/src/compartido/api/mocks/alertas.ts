// Errores simulados de /alerts/flood y /alerts/drought para ver los estados del diseño
// (?simular=rios-503|rios-502|sequia-503|400 en /alertas). Sin ?simular, todo sale del backend.
import type { RespuestaHttp, Simulacion } from "@/modulos/alertas/tipos";

const ERRORES: Record<number, { code: string; message: string }> = {
  400: { code: "UBICACION_FUERA_DE_COLOMBIA", message: "La ubicación está fuera de Colombia." },
  502: { code: "NIVEL_DESCONOCIDO", message: "La fuente envió un nivel de alerta desconocido." },
  503: { code: "FUENTE_NO_DISPONIBLE", message: "La fuente no respondió." },
};

const SIMULADOS: Record<Simulacion, { rios?: number; sequia?: number }> = {
  "400": { rios: 400, sequia: 400 },
  "rios-503": { rios: 503 },
  "rios-502": { rios: 502 },
  "sequia-503": { sequia: 503 },
};

/** La respuesta de error que pide ?simular para esa consulta; null si esa consulta va al backend. */
export function respuestaSimulada(consulta: "rios" | "sequia", simular?: Simulacion): RespuestaHttp | null {
  const estado = simular && SIMULADOS[simular][consulta];
  return estado ? { estado, cuerpo: ERRORES[estado] } : null;
}
