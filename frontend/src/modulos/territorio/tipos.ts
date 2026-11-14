// Tipos del módulo territorio. Códigos DIVIPOLA del DANE: 2 dígitos el departamento, 5 el municipio.

export interface Departamento {
  codigo: string;
  nombre: string;
}

export interface Municipio {
  codigo: string;
  nombre: string;
  /** "Municipio", "Área no municipalizada" o "Isla", según el DANE. */
  tipo?: string;
  lat?: number | null;
  lon?: number | null;
}

/** Departamento y municipio elegidos en un formulario. Vacío = sin elegir. */
export interface Ubicacion {
  departamento: string;
  municipio: string;
}

/** El código DIVIPOLA del municipio empieza por el de su departamento. */
export function departamentoDeMunicipio(codigoMunicipio: string): string {
  return codigoMunicipio.slice(0, 2);
}
