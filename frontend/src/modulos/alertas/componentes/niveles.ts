// Apariencia y mensajes de cada nivel (diseño de Figma: "Alertas IDEAM" y "Estados de la pantalla").
import type { FalloConsulta, NivelRios, NivelSequia } from "../tipos";

export type Tono = "verde" | "amarilla" | "naranja" | "roja" | "gris";

/** Clases de cada tono. `barra`/`barraTenue`: segmento vigente y los demás en la escala de niveles. */
export const TONOS: Record<Tono, { fondo: string; texto: string; barra: string; barraTenue: string }> = {
  verde: { fondo: "bg-verde-100", texto: "text-verde-700", barra: "bg-verde-500", barraTenue: "bg-verde-500/25" },
  amarilla: {
    fondo: "bg-amarilla-fondo",
    texto: "text-amarilla-texto",
    barra: "bg-amarilla",
    barraTenue: "bg-amarilla/25",
  },
  naranja: {
    fondo: "bg-naranja-fondo",
    texto: "text-naranja-texto",
    barra: "bg-naranja",
    barraTenue: "bg-naranja/25",
  },
  roja: { fondo: "bg-roja-fondo", texto: "text-roja-texto", barra: "bg-roja", barraTenue: "bg-roja/25" },
  gris: { fondo: "bg-fondo", texto: "text-tinta-suave", barra: "bg-apagado", barraTenue: "bg-apagado/25" },
};

/** Las cuatro casillas de la escala (Normal, Amarilla, Naranja, Roja), en orden. */
export const TONOS_ESCALA: Tono[] = ["verde", "amarilla", "naranja", "roja"];

interface Nivel {
  tono: Tono;
  /** Casilla de la escala que se resalta (0 = normal). */
  escala: number;
  etiqueta: string;
  /** Texto del chip en "Tus terrenos". */
  corto: string;
}

export interface NivelDeRios extends Nivel {
  titulo: string;
  accion: string | null;
  /** Icono del aviso (22 px). La alerta naranja trae en el diseño sus versiones de 36 px y 24 px (móvil). */
  icono: string;
  iconoMovil?: string;
}

export const NIVELES_RIOS: Record<NivelRios, NivelDeRios> = {
  SIN_ALERTA: {
    tono: "verde",
    escala: 0,
    etiqueta: "SIN ALERTA",
    corto: "Sin alerta",
    titulo: "El IDEAM no tiene alertas vigentes para los ríos y quebradas de tu zona.",
    accion: null,
    icono: "check-22",
  },
  NORMAL: {
    tono: "verde",
    escala: 0,
    etiqueta: "CONDICIONES NORMALES",
    corto: "Normal",
    titulo: "Sin alertas por ahora en los ríos y quebradas de tu zona.",
    accion: null,
    icono: "check-22",
  },
  AMARILLA: {
    tono: "amarilla",
    escala: 1,
    etiqueta: "ALERTA AMARILLA",
    corto: "Amarilla",
    titulo: "Atención: el IDEAM está vigilando los ríos y quebradas de tu zona.",
    accion: "Todavía no hay peligro, pero mantente pendiente.",
    icono: "alert-amarilla-22",
  },
  NARANJA: {
    tono: "naranja",
    escala: 2,
    etiqueta: "ALERTA NARANJA",
    corto: "Naranja",
    titulo: "Prepárate: los ríos y quebradas de tu zona pueden crecer pronto.",
    accion: "Aleja de la orilla animales, bombas y herramientas.",
    icono: "alert-naranja-36",
    iconoMovil: "alert-naranja-24",
  },
  ROJA: {
    tono: "roja",
    escala: 3,
    etiqueta: "ALERTA ROJA",
    corto: "Roja",
    titulo: "Peligro: hay riesgo alto de creciente o inundación en tu zona.",
    accion: "Aléjate de ríos y quebradas y pon a salvo a tu familia y tus animales.",
    icono: "alert-roja-22",
  },
};

export const ESCALA_RIOS = ["Normal", "Amarilla", "Naranja", "Roja"];

export interface NivelDeSequia extends Nivel {
  mensaje: string;
}

export const NIVELES_SEQUIA: Record<NivelSequia, NivelDeSequia> = {
  NORMAL: {
    tono: "verde",
    escala: 0,
    etiqueta: "SEQUÍA · NIVEL NORMAL",
    corto: "Normal",
    mensaje: "En el último mes ha llovido lo normal para estas fechas en tu ubicación.",
  },
  AMARILLA: {
    tono: "amarilla",
    escala: 1,
    etiqueta: "SEQUÍA · NIVEL AMARILLA",
    corto: "Amarilla",
    mensaje: "Ha llovido menos de lo normal en el último mes. Cuida el agua y revisa la humedad de tu suelo.",
  },
  NARANJA: {
    tono: "naranja",
    escala: 2,
    etiqueta: "SEQUÍA · NIVEL NARANJA",
    corto: "Naranja",
    mensaje: "Ha llovido mucho menos de lo normal en el último mes. Guarda agua y riega solo lo necesario.",
  },
  ROJA: {
    tono: "roja",
    escala: 3,
    etiqueta: "SEQUÍA · NIVEL ROJA",
    corto: "Roja",
    mensaje: "Casi no ha llovido en el último mes. Protege el agua para tu familia, tus animales y tus cultivos.",
  },
};

export const ESCALA_SEQUIA = ["Normal", "Menos lluvia", "Mucho menos", "Sequía fuerte"];

/** Avisos cuando no hay datos (respuestas 400, 502 y 503). */
export interface AvisoFallo {
  tono: Tono;
  icono: string;
  etiqueta: string;
  mensaje: string;
  reintentable: boolean;
}

const UBICACION_INVALIDA: AvisoFallo = {
  tono: "roja",
  icono: "pin-rojo-22",
  etiqueta: "UBICACIÓN NO VÁLIDA",
  mensaje: "La ubicación está fuera de Colombia.",
  reintentable: false,
};

export const FALLOS: Record<"rios" | "sequia", Record<FalloConsulta, AvisoFallo>> = {
  rios: {
    SIN_CONEXION: {
      tono: "gris",
      icono: "sin-conexion-22",
      etiqueta: "SIN CONEXIÓN CON EL IDEAM",
      mensaje: "No pudimos consultar al IDEAM. Intenta de nuevo en unos minutos.",
      reintentable: true,
    },
    NIVEL_DESCONOCIDO: {
      tono: "gris",
      icono: "alert-gris-22",
      etiqueta: "RESPUESTA NO RECONOCIDA",
      mensaje: "El IDEAM envió un nivel de alerta que no reconocemos.",
      reintentable: true,
    },
    UBICACION_INVALIDA,
  },
  sequia: {
    SIN_CONEXION: {
      tono: "gris",
      icono: "sin-conexion-22",
      etiqueta: "SIN DATOS DE LLUVIA",
      mensaje: "No pudimos consultar los datos de lluvia de la NASA. Intenta de nuevo en unos minutos.",
      reintentable: true,
    },
    NIVEL_DESCONOCIDO: {
      tono: "gris",
      icono: "alert-gris-22",
      etiqueta: "RESPUESTA NO RECONOCIDA",
      mensaje: "Recibimos un nivel de sequía que no reconocemos.",
      reintentable: true,
    },
    UBICACION_INVALIDA,
  },
};
