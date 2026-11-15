// Llamadas HTTP del módulo alertas. Usan el cliente de src/compartido/api/http.ts y exigen sesión.
//
//   GET /alerts/flood?lat=&lon=    alerta hidrológica vigente del IDEAM (módulo alertas del backend)
//   GET /alerts/drought?lat=&lon=  indicador propio de sequía con la lluvia de NASA POWER
//
// Los lotes son los terrenos reales del productor (módulo fincas); se consultan por su centroide.
// interpretarRios/interpretarSequia traducen los 200, 400, 502 y 503 al estado que muestra la pantalla.
import { peticion } from "@/compartido/api/http";
import { esErrorApi } from "@/compartido/api/tipos";
import { respuestaSimulada } from "@/compartido/api/mocks/alertas";
import { formatoArea, listarTerrenos, type Terreno } from "@/modulos/fincas";
import { interpretarRios, interpretarSequia } from "./respuestas";
import type { AlertaRios, Consulta, IndicadorSequia, LoteUbicado, RespuestaHttp, Simulacion } from "./tipos";

const SIMULACIONES: readonly Simulacion[] = ["rios-503", "rios-502", "sequia-503", "400"];

export function leerSimulacion(valor: string | string[] | undefined): Simulacion | undefined {
  return SIMULACIONES.find((s) => s === valor);
}

/** Terreno → lote de la pantalla, ubicado en el punto medio del polígono (el mismo que usa el planificador). */
export function aLoteUbicado(t: Pick<Terreno, "id" | "nombre" | "area_ha" | "centroide">): LoteUbicado {
  return { id: t.id, nombre: t.nombre, area: formatoArea(t.area_ha), lat: t.centroide.lat, lon: t.centroide.lon };
}

/** Lotes del usuario con su ubicación. */
export async function obtenerLotesUbicados(): Promise<LoteUbicado[]> {
  return (await listarTerrenos()).map(aLoteUbicado);
}

// http.ts convierte los errores en ErrorApi ({ codigo, mensaje }) y pierde el estado HTTP;
// el código del backend (contrato de co.surco.alertas.dominio.ErroresAlertas) dice cuál era.
const ESTADO_POR_CODIGO: Record<string, number> = {
  UBICACION_FUERA_DE_COLOMBIA: 400,
  NIVEL_DESCONOCIDO: 502,
  FUENTE_NO_DISPONIBLE: 503,
};

async function consultar(ruta: "/alerts/flood" | "/alerts/drought", lote: LoteUbicado): Promise<RespuestaHttp> {
  const consulta = new URLSearchParams({ lat: String(lote.lat), lon: String(lote.lon) });
  try {
    const cuerpo = await peticion<unknown>("GET", `${ruta}?${consulta}`, { autenticada: true });
    return { estado: 200, cuerpo };
  } catch (e) {
    // Red caída, sesión vencida o un error que no es de alertas: se muestra como "sin conexión" (503).
    const estado = esErrorApi(e) ? (ESTADO_POR_CODIGO[e.codigo] ?? 503) : 503;
    return { estado, cuerpo: e };
  }
}

export async function consultarRios(lote: LoteUbicado, simular?: Simulacion): Promise<Consulta<AlertaRios>> {
  return interpretarRios(respuestaSimulada("rios", simular) ?? (await consultar("/alerts/flood", lote)));
}

export async function consultarSequia(lote: LoteUbicado, simular?: Simulacion): Promise<Consulta<IndicadorSequia>> {
  return interpretarSequia(respuestaSimulada("sequia", simular) ?? (await consultar("/alerts/drought", lote)));
}
