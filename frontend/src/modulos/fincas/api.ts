// Llamadas HTTP del módulo fincas. Usan el cliente de src/compartido/api/http.ts y exigen sesión.
import { peticion } from "@/compartido/api/http";
import type { SolicitudTerreno, Terreno } from "./tipos";

/** GET /fincas/terrenos: los terrenos del productor, del más antiguo al más nuevo. */
export async function listarTerrenos(): Promise<Terreno[]> {
  return peticion<Terreno[]>("GET", "/fincas/terrenos", { autenticada: true });
}

/** GET /fincas/terrenos/{id}: 404 si no existe o es de otro productor. */
export async function obtenerTerreno(id: string): Promise<Terreno> {
  return peticion<Terreno>("GET", `/fincas/terrenos/${encodeURIComponent(id)}`, { autenticada: true });
}

/** POST /fincas/terrenos: guarda el contorno dibujado en el mapa. */
export async function crearTerreno(solicitud: SolicitudTerreno): Promise<Terreno> {
  return peticion<Terreno>("POST", "/fincas/terrenos", { cuerpo: solicitud, autenticada: true });
}

/** DELETE /fincas/terrenos/{id} */
export async function eliminarTerreno(id: string): Promise<void> {
  return peticion<void>("DELETE", `/fincas/terrenos/${encodeURIComponent(id)}`, { autenticada: true });
}
