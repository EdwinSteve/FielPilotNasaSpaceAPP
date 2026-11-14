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

export interface Rotacion {
  id: string;
  grado: GradoRotacion;
  /** Cultivos en el orden en que se siembran. */
  secuencia: string[];
  /** 8 = "8 de cada 10 años le va bien". */
  frecuenciaExito: number;
  aguaAhorrada: string;
  aporteNitrogeno: string | null;
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
  usuario: { nombreCompleto: string; iniciales: string; rol: string; municipio: string };
  alertasPendientes: number;
  finca: { nombre: string; municipio: string };
  lote: LoteOpcion;
  lotes: LoteOpcion[];
  /** Las mejores rotaciones, de la mejor a la peor (la pantalla muestra 3). */
  rotaciones: Rotacion[];
  ranking: CultivoRanking[];
  lluvias: { fuente: string; meses: LluviaMensual[] };
}
