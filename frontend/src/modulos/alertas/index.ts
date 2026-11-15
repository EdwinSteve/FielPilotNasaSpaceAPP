// API pública del módulo alertas: las páginas solo importan desde aquí.
export { leerSimulacion, obtenerLotesUbicados } from "./api";
export { AlertasConSesion } from "./componentes/AlertasConSesion";
export { PantallaAlertas } from "./componentes/PantallaAlertas";
export type { AlertaRios, IndicadorSequia, LoteUbicado, NivelRios, NivelSequia } from "./tipos";
