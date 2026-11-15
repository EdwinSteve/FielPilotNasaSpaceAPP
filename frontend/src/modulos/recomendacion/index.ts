// API pública del módulo recomendacion: las páginas solo importan desde aquí.
export { obtenerRecomendacion } from "./api";
export { RecomendacionConSesion } from "./componentes/PantallaRecomendacion";
export { deCadaDiez, fechaCorta, porcentaje } from "./formato";
export type { RespuestaRecomendacion, PlanCiclo, CultivoRecomendado, BloqueClima, CodigoErrorRecomendacion } from "./tipos";
