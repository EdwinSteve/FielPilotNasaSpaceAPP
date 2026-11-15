// API pública del módulo territorio: las páginas y otros módulos solo importan desde aquí.
export { listarDepartamentos, listarMunicipios, municipioCercano } from "./api";
export { useDepartamentos, useMunicipios } from "./hooks/useMunicipios";
export { SelectorUbicacion } from "./componentes/SelectorUbicacion";
export { departamentoDeMunicipio } from "./tipos";
export type { Departamento, Municipio, Ubicacion } from "./tipos";
