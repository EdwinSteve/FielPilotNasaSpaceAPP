// Llamadas HTTP del módulo rotacion. Usan el cliente de src/compartido/api/http.ts.
import { lotesMock, planRotacionMock } from "@/compartido/api/mocks/rotacion";
import type { PlanRotacion } from "./tipos";

// Por ahora devuelve los datos de ejemplo del diseño; se reemplaza por la llamada
// al backend cuando existan los endpoints (plan del lote, lotes del usuario y lluvias esperadas).
// Devuelve null si el lote no es del usuario (el backend responde 404).
export async function obtenerPlanRotacion(loteId: string): Promise<PlanRotacion | null> {
  const lote = lotesMock.find((l) => l.id === loteId);
  return lote ? planRotacionMock(lote) : null;
}
