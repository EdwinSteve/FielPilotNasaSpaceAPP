// Tipos del módulo rotacion: reflejan los schemas del backend.
// Los valores llegan ya formateados para mostrarse (unidades y meses en español).

export type GradoRotacion = "A+" | "A" | "B" | "C" | "D";
export type NivelLluvia = "seco" | "normal" | "lluvioso";

/** Un lote del usuario, para el selector. */
export interface LoteOpcion {
  id: string;
  nombre: string;
  area: string;
}

/**
 * Una temporada de la rotación. La primera se evalúa con el pronóstico de los próximos meses
 * ("pronostico"); las siguientes, con lo que pasó en años anteriores en el terreno ("historico").
 */
export interface TemporadaRotacion {
  cultivo: string;
  nombre: string;
  /** AAAA-MM-DD */
  siembra: string;
  cosecha: string;
  probExito: number;
  fuente: "pronostico" | "historico";
}

export interface Rotacion {
  id: string;
  grado: GradoRotacion;
  /** Cultivos en el orden en que se siembran. */
  secuencia: string[];
  /** 8 = "8 de cada 10 años le va bien". */
  frecuenciaExito: number;
  aguaAhorrada: string;
  aporteNitrogeno: string | null;
  /** Fechas de siembra y cosecha de cada temporada (vacío en los datos de ejemplo). */
  temporadas: TemporadaRotacion[];
}

/** El ciclo que eligió el agricultor: el plan activo del lote. */
export interface SeleccionCiclo {
  rotacionId: string;
  secuencia: string[];
  /** Fecha y hora ISO. */
  elegidaEn: string;
}

/** Qué tanto le conviene un cultivo al lote, de 0 a 1. */
export interface CultivoRanking {
  id: string;
  nombre: string;
  puntaje: number;
  apto: boolean;
  razonNoApto: string | null;
}

export interface LluviaMensual {
  mes: string;
  lluviaMm: number;
  nivel: NivelLluvia;
  nota: string | null;
}

export interface PlanRotacion {
  /** Id del plan guardado en el backend; la selección apunta a él. */
  planId: string;
  usuario: { nombreCompleto: string; iniciales: string; rol: string; municipio: string };
  alertasPendientes: number;
  finca: { nombre: string; municipio: string };
  lote: LoteOpcion;
  lotes: LoteOpcion[];
  /** Las mejores rotaciones, de la mejor a la peor (la pantalla muestra 3). */
  rotaciones: Rotacion[];
  ranking: CultivoRanking[];
  lluvias: { fuente: string; meses: LluviaMensual[] };
  seleccion: SeleccionCiclo | null;
}

// --- Respuestas del backend (snake_case), ver co.surco.rotacion.api.RotacionesControlador ---

export interface RespuestaSeleccion {
  plan_id: string;
  rotacion_id: string;
  secuencia: string[];
  elegida_en: string;
}

export interface RespuestaPlanRotacion {
  id: string;
  terreno_id: string;
  recomendacion_id: string;
  creado_en: string;
  nombre_terreno: string;
  area_ha: number;
  /** "bf" (oráculo sobre el pronóstico) o "red". */
  motor: string;
  fuente_historico: string;
  rotaciones: {
    id: string;
    grado: GradoRotacion;
    secuencia: string[];
    temporadas: { cultivo: string; nombre: string; siembra: string; cosecha: string; prob_exito: number; fuente: "pronostico" | "historico" }[];
    puntaje: number;
    frecuencia_exito: number;
    agua_ahorrada: string;
    agua_ahorrada_m3: number;
    aporte_nitrogeno: string | null;
  }[];
  ranking: { id: string; nombre: string; puntaje: number; apto: boolean; razon_no_apto: string | null }[];
  lluvias: { fuente: string; meses: { mes: string; lluvia_mm: number; nivel: NivelLluvia; nota: string | null }[] };
  seleccion: RespuestaSeleccion | null;
}

/**
 * Códigos de error que esta pantalla trata aparte (co.surco.rotacion y co.surco.recomendacion).
 * No los cambie sin cambiarlos allá.
 */
export type CodigoErrorRotacion =
  | "ROTACION_SIN_CALCULAR"
  | "ROTACION_NO_ENCONTRADA"
  | "RECOMENDACION_TERRENO_NO_ENCONTRADO"
  | "RECOMENDACION_TERRENO_SIN_SUELO"
  | "RECOMENDACION_MES_NO_ENTRENADO"
  | "RECOMENDACION_FUENTE_CAIDA"
  | "RECOMENDACION_NO_DISPONIBLE";
