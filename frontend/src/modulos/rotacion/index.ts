// API pública del módulo rotacion: las páginas solo importan desde aquí.
export { obtenerPlanRotacion, elegirRotacion, aPlanRotacion, aLoteOpcion } from "./api";
export { usePlanRotacion } from "./hooks/usePlanRotacion";
export { PantallaRotacion } from "./componentes/PantallaRotacion";
export { RotacionConSesion } from "./componentes/RotacionConSesion";
export { SelectorLote } from "./componentes/SelectorLote";
export type { LoteOpcion, PlanRotacion, SeleccionCiclo, RespuestaPlanRotacion } from "./tipos";
