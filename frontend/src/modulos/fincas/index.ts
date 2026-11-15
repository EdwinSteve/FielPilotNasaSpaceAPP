// API pública del módulo fincas: las páginas solo importan desde aquí.
export { listarTerrenos, obtenerTerreno, crearTerreno, eliminarTerreno } from "./api";
export { FormularioTerreno } from "./componentes/FormularioTerreno";
export { TerrenosConSesion, NuevoTerrenoConSesion } from "./componentes/PantallasTerreno";
export { medir, problemaDelContorno, formatoArea } from "./geometria";
export type { Terreno, SolicitudTerreno, Vertice, CodigoErrorFincas } from "./tipos";
