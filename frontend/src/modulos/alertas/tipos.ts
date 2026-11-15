// Tipos del módulo alertas: reflejan los schemas del backend.
// Dos consultas por ubicación (latitud y longitud) de cada lote:
//   GET /alerts/flood    alerta hidrológica del IDEAM para la subzona hidrográfica
//   GET /alerts/drought  indicador propio de sequía con la lluvia de NASA POWER

export const NIVELES_RIOS = ["SIN_ALERTA", "NORMAL", "AMARILLA", "NARANJA", "ROJA"] as const;
export const NIVELES_SEQUIA = ["NORMAL", "AMARILLA", "NARANJA", "ROJA"] as const;

/** SIN_ALERTA: el IDEAM no tiene alerta para la subzona. NORMAL: la tiene, en condiciones normales. */
export type NivelRios = (typeof NIVELES_RIOS)[number];
export type NivelSequia = (typeof NIVELES_SEQUIA)[number];

export interface AlertaRios {
  nivel: NivelRios;
  /** Subzona hidrográfica del IDEAM; null si no hay alerta para la ubicación. */
  subzona: string | null;
  /** Tipo de alerta del IDEAM (por ejemplo, "Hidrológica"). */
  tipo: string | null;
  lat: number;
  lon: number;
  /** Fecha y hora ISO de la consulta. */
  consultadoEn: string;
}

export interface IndicadorSequia {
  nivel: NivelSequia;
  lat: number;
  lon: number;
  consultadoEn: string;
}

/**
 * Por qué no hay datos:
 * - SIN_CONEXION: la fuente (IDEAM o NASA POWER) no respondió (503) o falló la red.
 * - NIVEL_DESCONOCIDO: la fuente envió un nivel que no reconocemos (502, o un 200 con otro nivel).
 * - UBICACION_INVALIDA: la ubicación está fuera de Colombia (400).
 */
export type FalloConsulta = "SIN_CONEXION" | "NIVEL_DESCONOCIDO" | "UBICACION_INVALIDA";

export type Consulta<T> =
  | { estado: "cargando" }
  | { estado: "ok"; datos: T }
  | { estado: "error"; fallo: FalloConsulta };

/** Un lote del usuario con la ubicación por la que se consultan sus alertas. */
export interface LoteUbicado {
  id: string;
  nombre: string;
  area: string;
  lat: number;
  lon: number;
}

export interface AlertasLote {
  rios: Consulta<AlertaRios>;
  sequia: Consulta<IndicadorSequia>;
}

/** Respuesta HTTP sin interpretar: el cliente real y el de ejemplo pasan por el mismo intérprete. */
export interface RespuestaHttp {
  estado: number;
  cuerpo: unknown;
}

/** Fuerza una respuesta de error en los datos de ejemplo (?simular=... en /alertas). */
export type Simulacion = "rios-503" | "rios-502" | "sequia-503" | "400";
