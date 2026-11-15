// Tipos del módulo fincas: reflejan los schemas del backend (co.surco.fincas.api.Dtos), en snake_case.

/** Un punto en grados decimales (WGS 84, el de GPS y Leaflet). */
export interface Vertice {
  lat: number;
  lon: number;
}

/** POST /fincas/terrenos */
export interface SolicitudTerreno {
  nombre: string;
  municipio: string;
  riego: boolean;
  /** Las esquinas en orden, sin repetir la primera al final. */
  vertices: Vertice[];
}

/** Respuesta de GET y POST /fincas/terrenos */
export interface Terreno {
  id: string;
  nombre: string;
  municipio: string;
  departamento: string;
  riego: boolean;
  area_ha: number;
  /** Punto medio del polígono: el que usa el planificador estacional. */
  centroide: Vertice;
  vertices: Vertice[];
  /** GeoJSON (RFC 7946, [lon, lat]) para mapas y SIG. */
  poligono_geojson: string;
  creado_en: string;
}

/** Códigos de error de fincas (co.surco.fincas.dominio.ErroresFincas). No los cambie sin cambiarlos allá. */
export type CodigoErrorFincas =
  | "FINCAS_NOMBRE_INVALIDO"
  | "FINCAS_MUNICIPIO_INEXISTENTE"
  | "FINCAS_COORDENADA_INVALIDA"
  | "FINCAS_POLIGONO_INCOMPLETO"
  | "FINCAS_POLIGONO_DEMASIADOS_PUNTOS"
  | "FINCAS_FUERA_DE_COLOMBIA"
  | "FINCAS_POLIGONO_CRUZADO"
  | "FINCAS_AREA_MUY_PEQUENA"
  | "FINCAS_AREA_MUY_GRANDE"
  | "FINCAS_LIMITE_TERRENOS"
  | "FINCAS_TERRENO_NO_ENCONTRADO";
