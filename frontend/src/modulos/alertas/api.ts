// Llamadas HTTP del módulo alertas. Usan el cliente de src/compartido/api/http.ts.
//
// Por ahora devuelven las respuestas de ejemplo del diseño. Cuando existan los endpoints
// (GET /alerts/flood?lat=&lon= y GET /alerts/drought?lat=&lon=), solo cambia de dónde sale la
// RespuestaHttp: interpretarRios/interpretarSequia ya traducen los 200, 400, 502 y 503.
import {
  lotesUbicadosMock,
  respuestaRiosMock,
  respuestaSequiaMock,
} from "@/compartido/api/mocks/alertas";
import { interpretarRios, interpretarSequia } from "./respuestas";
import type { AlertaRios, Consulta, IndicadorSequia, LoteUbicado, Simulacion } from "./tipos";

const SIMULACIONES: readonly Simulacion[] = ["rios-503", "rios-502", "sequia-503", "400"];

export function leerSimulacion(valor: string | string[] | undefined): Simulacion | undefined {
  return SIMULACIONES.find((s) => s === valor);
}

/** Lotes del usuario con su ubicación. */
export async function obtenerLotesUbicados(): Promise<LoteUbicado[]> {
  return lotesUbicadosMock;
}

export async function consultarRios(lote: LoteUbicado, simular?: Simulacion): Promise<Consulta<AlertaRios>> {
  try {
    return interpretarRios(await respuestaRiosMock(lote, simular));
  } catch {
    return { estado: "error", fallo: "SIN_CONEXION" };
  }
}

export async function consultarSequia(lote: LoteUbicado, simular?: Simulacion): Promise<Consulta<IndicadorSequia>> {
  try {
    return interpretarSequia(await respuestaSequiaMock(lote, simular));
  } catch {
    return { estado: "error", fallo: "SIN_CONEXION" };
  }
}
