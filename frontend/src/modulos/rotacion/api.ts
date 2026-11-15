// Llamadas HTTP del módulo rotacion. Usan el cliente de src/compartido/api/http.ts y exigen sesión.
//
//   GET  /lotes/{id}/rotaciones   el último plan calculado (404 ROTACION_SIN_CALCULAR si no hay)
//   POST /lotes/{id}/rotaciones   lo calcula con el modelo (la primera vez en una zona tarda minutos)
//   PUT  /lotes/{id}/seleccion    guarda el ciclo elegido
//
// Los lotes del selector son los terrenos reales del productor (módulo fincas).
import { peticion } from "@/compartido/api/http";
import type { ErrorApi } from "@/compartido/api/tipos";
import { formatoArea, listarTerrenos, type Terreno } from "@/modulos/fincas";
import { listarMunicipios } from "@/modulos/territorio";
import type { LoteOpcion, PlanRotacion, RespuestaPlanRotacion, RespuestaSeleccion, SeleccionCiclo } from "./tipos";

const ruta = (loteId: string, resto: string) => `/lotes/${encodeURIComponent(loteId)}${resto}`;

/** Terreno → opción del selector ("El Mirador · 4,2 ha"). */
export function aLoteOpcion(t: Pick<Terreno, "id" | "nombre" | "area_ha">): LoteOpcion {
  return { id: t.id, nombre: t.nombre, area: formatoArea(t.area_ha) };
}

export async function listarLotes(): Promise<{ lotes: LoteOpcion[]; terrenos: Terreno[] }> {
  const terrenos = await listarTerrenos();
  return { lotes: terrenos.map(aLoteOpcion), terrenos };
}

export function aSeleccion(s: RespuestaSeleccion | null): SeleccionCiclo | null {
  return s && { rotacionId: s.rotacion_id, secuencia: s.secuencia, elegidaEn: s.elegida_en };
}

/** Lo que la pantalla sabe del lote y la finca además del plan. */
export interface ContextoPlan {
  lote: LoteOpcion;
  lotes: LoteOpcion[];
  municipio: string;
}

/** La respuesta del backend en la forma de la pantalla. El usuario lo pone la sesión (RotacionConSesion). */
export function aPlanRotacion(r: RespuestaPlanRotacion, { lote, lotes, municipio }: ContextoPlan): PlanRotacion {
  return {
    planId: r.id,
    usuario: { nombreCompleto: "", iniciales: "", rol: "", municipio: "" },
    alertasPendientes: 0,
    finca: { nombre: r.nombre_terreno, municipio },
    lote,
    lotes,
    rotaciones: r.rotaciones.map((x) => ({
      id: x.id,
      grado: x.grado,
      secuencia: x.secuencia,
      frecuenciaExito: x.frecuencia_exito,
      aguaAhorrada: x.agua_ahorrada,
      aporteNitrogeno: x.aporte_nitrogeno,
      temporadas: x.temporadas.map((t) => ({
        cultivo: t.cultivo,
        nombre: t.nombre,
        siembra: t.siembra,
        cosecha: t.cosecha,
        probExito: t.prob_exito,
        fuente: t.fuente,
      })),
    })),
    // Solo los cultivos que puntúa el modelo estacional: nunca perennes como café o cacao
    ranking: r.ranking.map((c) => ({
      id: c.id,
      nombre: c.nombre,
      puntaje: c.puntaje,
      apto: c.apto,
      razonNoApto: c.razon_no_apto,
    })),
    lluvias: {
      fuente: r.lluvias.fuente,
      meses: r.lluvias.meses.map((m) => ({ mes: m.mes, lluviaMm: m.lluvia_mm, nivel: m.nivel, nota: m.nota })),
    },
    seleccion: aSeleccion(r.seleccion),
  };
}

const esCodigo = (e: unknown, codigo: string) => (e as ErrorApi | undefined)?.codigo === codigo;

/** El último plan guardado del lote; null si todavía no se ha calculado. */
export async function obtenerPlanGuardado(loteId: string): Promise<RespuestaPlanRotacion | null> {
  try {
    return await peticion<RespuestaPlanRotacion>("GET", ruta(loteId, "/rotaciones"), { autenticada: true });
  } catch (e) {
    if (esCodigo(e, "ROTACION_SIN_CALCULAR")) return null;
    throw e;
  }
}

/** Calcula el plan con el modelo. La primera consulta de una zona puede tardar 1-3 minutos. */
export async function calcularPlan(loteId: string): Promise<RespuestaPlanRotacion> {
  return peticion<RespuestaPlanRotacion>("POST", ruta(loteId, "/rotaciones"), { autenticada: true });
}

/** Guarda el ciclo elegido como plan activo del lote. */
export async function elegirRotacion(loteId: string, planId: string, rotacionId: string): Promise<SeleccionCiclo> {
  const s = await peticion<RespuestaSeleccion>("PUT", ruta(loteId, "/seleccion"), {
    cuerpo: { plan_id: planId, rotacion_id: rotacionId },
    autenticada: true,
  });
  return aSeleccion(s)!;
}

/** Nombre del municipio del terreno ("Neiva"); si no se puede consultar, el código DIVIPOLA. */
export async function nombreMunicipio(t: Pick<Terreno, "municipio" | "departamento">): Promise<string> {
  try {
    const municipios = await listarMunicipios(t.departamento);
    return municipios.find((m) => m.codigo === t.municipio)?.nombre ?? t.municipio;
  } catch {
    return t.municipio;
  }
}

/**
 * El plan del lote: el guardado o, si no hay, uno recién calculado. null si el lote no existe o es de
 * otro productor (como hoy: la página muestra "no encontrado").
 */
export async function obtenerPlanRotacion(loteId: string): Promise<PlanRotacion | null> {
  const { lotes, terrenos } = await listarLotes();
  const terreno = terrenos.find((t) => t.id === loteId);
  if (!terreno) return null;
  const [respuesta, municipio] = await Promise.all([
    obtenerPlanGuardado(loteId).then((p) => p ?? calcularPlan(loteId)),
    nombreMunicipio(terreno),
  ]);
  return aPlanRotacion(respuesta, { lote: aLoteOpcion(terreno), lotes, municipio });
}
