// Tipos del módulo inicio: reflejan los schemas del backend.
// Los valores llegan ya formateados para mostrarse (unidades y fechas en español).

export type NivelAlerta = "amarilla" | "naranja";
export type IconoClima = "rain" | "cloudsun" | "sun";
export type IconoMetrica = "thermo" | "rain" | "drop" | "wind" | "sun" | "layers";

export interface UsuarioInicio {
  nombre: string;
  nombreCompleto: string;
  iniciales: string;
  rol: string;
  municipio: string;
}

export interface LoteInicio {
  id: string;
  nombre: string;
  area: string;
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

export interface AlertaDestacada {
  nivel: NivelAlerta;
  titulo: string;
  descripcion: string;
  vigencia: string;
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

export interface AlertaResumen {
  nivel: NivelAlerta;
  titulo: string;
  detalle: string;
}

export interface RecomendacionLote {
  titulo: string;
  descripcion: string;
  rotacion: string;
}

export interface ResumenInicio {
  usuario: UsuarioInicio;
  /** La misma fecha en tres largos: escritorio, tablet y móvil. */
  fecha: { larga: string; media: string; corta: string };
  finca: { nombre: string; municipio: string; departamento: string };
  lote: LoteInicio;
  clima: ClimaActual;
  alertaDestacada: AlertaDestacada;
  metricas: Metrica[];
  pronostico: DiaPronostico[];
  alertas: AlertaResumen[];
  alertasPendientes: number;
  recomendacion: RecomendacionLote;
}
