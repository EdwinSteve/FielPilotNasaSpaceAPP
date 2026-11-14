// Llamadas HTTP del módulo inicio. Usan el cliente de src/compartido/api/http.ts.
import { resumenInicioMock } from "@/compartido/api/mocks/inicio";
import type { ResumenInicio } from "./tipos";

// Por ahora devuelve los datos de ejemplo del diseño; se reemplaza por la llamada
// al backend cuando exista el endpoint.
export async function obtenerResumenInicio(): Promise<ResumenInicio> {
  return resumenInicioMock;
}
