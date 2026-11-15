// Tipos del módulo inicio.
// ResumenClimaApi refleja el schema del backend (GET /clima/resumen, módulo clima);
// ResumenInicio es lo que pintan los componentes: valores ya formateados (unidades y fechas en español).

// --- Backend ---

export type NivelApi = "AMARILLA" | "NARANJA" | "ROJA";
export type IconoClima = "rain" | "cloudsun" | "sun";

export interface AvisoApi {
  tipo: "RIOS" | "LLUVIA_FUERTE" | "SEQUIA";
  nivel: NivelApi;
  titulo: string;
  mensaje: string;
  zona: string | null;
  fuente: string;
  /** true solo para las alertas que emite el IDEAM; las demás las calcula la app. */
  oficial: boolean;
}

export interface DiaApi {
  fecha: string;
  descripcion: string;
  icono: IconoClima;
  temp_max: number;
  temp_min: number;
  lluvia_mm: number;
  prob_lluvia: number | null;
  indice_uv: number | null;
  lluvia_fuerte: boolean;
}

/** Un campo en null significa "sin dato reciente", nunca cero. */
export interface ResumenClimaApi {
  ubicacion: { municipio: string | null; departamento: string | null; lat: number; lon: number; altitud_m: number | null };
  actualizado: string;
  actual: {
    temperatura: number;
    sensacion: number | null;
    descripcion: string;
    icono: IconoClima;
    humedad: number | null;
    viento_kmh: number | null;
    viento_rumbo: string | null;
    rafagas_kmh: number | null;
    lluvia_dia_mm: number;
    lluvia_semana_mm: number;
  };
  /** null si NASA POWER no respondió. */
  satelite: {
    radiacion_kwh: number | null;
    radiacion_fecha: string | null;
    humedad_suelo_pct: number | null;
    humedad_suelo_fecha: string | null;
  } | null;
  /** Desde hoy. */
  pronostico: DiaApi[];
  /** Del más grave al menos grave. */
  avisos: AvisoApi[];
  fuentes: string[];
}

// --- Pantalla ---

export type NivelAlerta = "amarilla" | "naranja" | "roja";
export type IconoMetrica = "thermo" | "rain" | "drop" | "wind" | "sun" | "layers";

export interface UsuarioInicio {
  nombre: string;
  nombreCompleto: string;
  iniciales: string;
  rol: string;
  municipio: string;
}

/** Punto del que se muestra el clima: hoy la cabecera del municipio del usuario. */
export interface UbicacionInicio {
  nombre: string;
  departamento: string;
  coordenadas: string;
  altitud: string;
}

export interface ClimaActual {
  temperatura: string;
  condicion: string;
  detalle: string;
  lluviaHoy: string;
  fuentes: string;
}

export interface AlertaResumen {
  nivel: NivelAlerta;
  titulo: string;
  detalle: string;
  fuente: string;
  oficial: boolean;
}

export interface Metrica {
  icono: IconoMetrica;
  nombre: string;
  valor: string;
  unidad: string;
  detalle: string;
  fuente: string;
}

export interface DiaPronostico {
  dia: string;
  icono: IconoClima;
  temperaturas: string;
  lluviaMm: number;
  lluviaFuerte: boolean;
}

export interface RecomendacionLote {
  titulo: string;
  descripcion: string;
  rotacion: string;
}

export interface ResumenInicio {
  usuario: UsuarioInicio;
  /** "Buenos días", "Buenas tardes" o "Buenas noches", según la hora de Colombia. */
  saludo: string;
  /** La misma fecha en tres largos: escritorio, tablet y móvil. */
  fecha: { larga: string; media: string; corta: string };
  ubicacion: UbicacionInicio;
  /** Lote sobre el que abre "Cultivos y rotación". */
  loteId: string;
  clima: ClimaActual;
  /** La alerta o aviso más grave; null si no hay ninguno. */
  alertaDestacada: AlertaResumen | null;
  /** Lo que se dice en el banner cuando no hay alertas. */
  sinAlertas: string;
  metricas: Metrica[];
  pronostico: DiaPronostico[];
  alertas: AlertaResumen[];
  alertasPendientes: number;
  /** null hasta que el módulo de recomendación entregue la del terreno del usuario. */
  recomendacion: RecomendacionLote | null;
}

/** Lo que sale del backend de clima; el usuario lo pone la sesión. */
export type DatosInicio = Omit<ResumenInicio, "usuario">;
