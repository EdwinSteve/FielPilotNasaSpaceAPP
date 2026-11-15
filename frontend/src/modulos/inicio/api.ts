// Llamadas HTTP del módulo inicio contra la API de Spring Boot (backend/, módulo clima).
import { peticion } from "@/compartido/api/http";
import { obtenerMiPerfil } from "@/modulos/identidad";
import { aDatosInicio } from "./mapeo";
import type { DatosInicio, ResumenClimaApi } from "./tipos";

/** GET /clima/resumen?municipio=41001: tiempo de hoy, pronóstico de 7 días y avisos. Ruta protegida. */
export async function obtenerClima(municipio: string, punto?: { lat: number; lon: number }): Promise<ResumenClimaApi> {
  const consulta = new URLSearchParams({ municipio });
  if (punto) {
    consulta.set("lat", String(punto.lat));
    consulta.set("lon", String(punto.lon));
  }
  return peticion<ResumenClimaApi>("GET", `/clima/resumen?${consulta}`, { autenticada: true });
}

/**
 * Datos de la pantalla de inicio. Hoy el clima es el de la cabecera del municipio de residencia del usuario;
 * cuando el módulo fincas entregue sus terrenos, aquí se pasa el centro del terreno elegido a obtenerClima.
 */
export async function obtenerResumenInicio(): Promise<DatosInicio> {
  const perfil = await obtenerMiPerfil();
  return aDatosInicio(await obtenerClima(perfil.municipio_residencia));
}
