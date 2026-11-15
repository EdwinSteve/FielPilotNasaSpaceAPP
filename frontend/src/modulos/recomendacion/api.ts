// Llamadas HTTP del módulo recomendacion. Usan el cliente de src/compartido/api/http.ts y exigen sesión.
import { peticion } from "@/compartido/api/http";
import type { RespuestaRecomendacion } from "./tipos";

/**
 * GET /recomendacion/terrenos/{id}: el mejor ciclo de cultivo del terreno con el clima esperado.
 * La primera vez para una zona puede tardar 1-2 minutos (el modelo baja 40 años de clima y el suelo).
 */
export async function obtenerRecomendacion(terrenoId: string): Promise<RespuestaRecomendacion> {
  return peticion<RespuestaRecomendacion>("GET", `/recomendacion/terrenos/${encodeURIComponent(terrenoId)}`, {
    autenticada: true,
  });
}
