// Interpreta las respuestas de /alerts/flood y /alerts/drought y da formato a sus datos.
// Es la única puerta de entrada de los niveles: lo que no reconoce se muestra como "respuesta no reconocida".
import {
  NIVELES_RIOS,
  NIVELES_SEQUIA,
  type AlertaRios,
  type AlertasLote,
  type Consulta,
  type FalloConsulta,
  type IndicadorSequia,
  type RespuestaHttp,
} from "./tipos";

interface CuerpoBackend {
  nivel?: unknown;
  subzona?: unknown;
  tipo?: unknown;
  lat?: unknown;
  lon?: unknown;
  consultado_en?: unknown;
}

function falloPorEstado(estado: number): FalloConsulta {
  if (estado === 400) return "UBICACION_INVALIDA";
  if (estado === 502) return "NIVEL_DESCONOCIDO";
  return "SIN_CONEXION";
}

const texto = (v: unknown) => (typeof v === "string" && v.trim() ? v : null);
const numero = (v: unknown) => (typeof v === "number" && Number.isFinite(v) ? v : null);

type Base =
  | { valida: false; fallo: FalloConsulta }
  | { valida: true; cuerpo: CuerpoBackend; lat: number; lon: number; consultadoEn: string };

function base(r: RespuestaHttp): Base {
  if (r.estado !== 200) return { valida: false, fallo: falloPorEstado(r.estado) };
  const c = (r.cuerpo ?? {}) as CuerpoBackend;
  const lat = numero(c.lat);
  const lon = numero(c.lon);
  const consultadoEn = texto(c.consultado_en);
  if (lat === null || lon === null || consultadoEn === null) return { valida: false, fallo: "NIVEL_DESCONOCIDO" };
  return { valida: true, cuerpo: c, lat, lon, consultadoEn };
}

export function interpretarRios(r: RespuestaHttp): Consulta<AlertaRios> {
  const b = base(r);
  if (!b.valida) return { estado: "error", fallo: b.fallo };
  const nivel = NIVELES_RIOS.find((n) => n === b.cuerpo.nivel);
  if (!nivel) return { estado: "error", fallo: "NIVEL_DESCONOCIDO" };
  const sinAlerta = nivel === "SIN_ALERTA";
  return {
    estado: "ok",
    datos: {
      nivel,
      subzona: sinAlerta ? null : texto(b.cuerpo.subzona),
      tipo: sinAlerta ? null : texto(b.cuerpo.tipo),
      lat: b.lat,
      lon: b.lon,
      consultadoEn: b.consultadoEn,
    },
  };
}

export function interpretarSequia(r: RespuestaHttp): Consulta<IndicadorSequia> {
  const b = base(r);
  if (!b.valida) return { estado: "error", fallo: b.fallo };
  const nivel = NIVELES_SEQUIA.find((n) => n === b.cuerpo.nivel);
  if (!nivel) return { estado: "error", fallo: "NIVEL_DESCONOCIDO" };
  return { estado: "ok", datos: { nivel, lat: b.lat, lon: b.lon, consultadoEn: b.consultadoEn } };
}

/** Un lote cuenta como "con alerta" si sus ríos están en amarilla o más, o si hay algún nivel de sequía. */
export function tieneAlerta({ rios, sequia }: AlertasLote): boolean {
  const enRios = rios.estado === "ok" && ["AMARILLA", "NARANJA", "ROJA"].includes(rios.datos.nivel);
  const enSequia = sequia.estado === "ok" && sequia.datos.nivel !== "NORMAL";
  return enRios || enSequia;
}

const coordenada = new Intl.NumberFormat("es-CO", { minimumFractionDigits: 4, maximumFractionDigits: 4 });

/** (2.9273, -75.2819) → "2,9273° N, 75,2819° O" */
export function formatearUbicacion(lat: number, lon: number): string {
  const ns = lat >= 0 ? "N" : "S";
  const eo = lon >= 0 ? "E" : "O";
  return `${coordenada.format(Math.abs(lat))}° ${ns}, ${coordenada.format(Math.abs(lon))}° ${eo}`;
}

const hora = new Intl.DateTimeFormat("es-CO", { hour: "numeric", minute: "2-digit", hour12: true });
const dia = new Intl.DateTimeFormat("es-CO", { day: "numeric", month: "short" });
// Intl separa "a. m." con espacios de no separación; se dejan espacios normales.
const espacios = (s: string) => s.replace(/[\u00a0\u202f]/g, " ");

/** Fecha ISO → "Hoy, 10:32 a. m." o "14 de nov, 10:32 a. m." */
export function formatearConsulta(iso: string, ahora = new Date()): string {
  const fecha = new Date(iso);
  if (Number.isNaN(fecha.getTime())) return "—";
  const mismoDia = fecha.toDateString() === ahora.toDateString();
  return espacios(`${mismoDia ? "Hoy" : dia.format(fecha)}, ${hora.format(fecha)}`);
}
