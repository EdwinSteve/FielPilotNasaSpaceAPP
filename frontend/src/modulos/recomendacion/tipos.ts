// Tipos del módulo recomendacion: reflejan GET /recomendacion/terrenos/{id} (co.surco.recomendacion), en snake_case.
// Los rendimientos son relativos al potencial del cultivo: 1 = el máximo en buenas condiciones.

export interface TerrenoEvaluado {
  id: string;
  nombre: string;
  municipio: string;
  lat: number;
  lon: number;
  area_ha: number;
  riego: boolean;
}

/** Clima esperado en 30 días: lluvia del escenario seco (p10), típico (p50) y húmedo (p90) frente a lo normal. */
export interface BloqueClima {
  desde: string;
  hasta: string;
  lluvia_mm_p10: number;
  lluvia_mm_p50: number;
  lluvia_mm_p90: number;
  lluvia_normal_mm: number;
  anomalia_lluvia_pct: number;
  tmax_c: number;
  tmin_c: number;
  tmax_normal_c: number;
}

export interface CultivoRecomendado {
  cultivo: string;
  nombre: string;
  ciclo_dias: number;
  cosecha: string;
  rend_p10: number;
  rend_p50: number;
  rend_p90: number;
  /** Probabilidad de lograr al menos el 60 % del rendimiento potencial. */
  prob_exito: number;
  riego_mm_p50: number;
  rend_con_riego_p50: number;
  apto: boolean;
  razon_no_apto: string | null;
}

export interface OpcionSiembra {
  siembra: string;
  inicio_pronostico: string;
  cultivos: CultivoRecomendado[];
}

export interface PlanCiclo {
  lote: { lat: number; lon: number; elevacion_m: number; ph_suelo: number; riego: boolean };
  generado: string;
  pronostico: {
    /** "analogos": años parecidos de NASA POWER según El Niño/La Niña; "c3s": SEAS5. */
    fuente: string;
    oni: number;
    oni_tendencia: number;
    fase_enso: "nino" | "nina" | "neutral";
    panorama: BloqueClima[];
  };
  mejor: { siembra: string; cultivo: CultivoRecomendado } | null;
  opciones: OpcionSiembra[];
  modelo: string;
}

export interface RespuestaRecomendacion {
  terreno: TerrenoEvaluado;
  plan: PlanCiclo;
}

/** Códigos de error de recomendación (co.surco.recomendacion.dominio.ErroresRecomendacion). */
export type CodigoErrorRecomendacion =
  | "RECOMENDACION_TERRENO_NO_ENCONTRADO"
  | "RECOMENDACION_NO_DISPONIBLE"
  | "RECOMENDACION_TERRENO_SIN_DATOS";
