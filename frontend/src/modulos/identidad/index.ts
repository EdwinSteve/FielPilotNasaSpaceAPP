// API pública del módulo identidad: las páginas solo importan desde aquí.
export { Bienvenida } from "./componentes/Bienvenida";
export { FormularioIngreso } from "./componentes/FormularioIngreso";
export { FormularioRegistro } from "./componentes/FormularioRegistro";
export { TextoPolitica } from "./componentes/TextoPolitica";
export { iniciarSesion, registrarUsuario, cerrarSesion, obtenerPoliticaVigente, obtenerMiPerfil } from "./api";
export { haySesion } from "@/compartido/api/sesion";
export type { Usuario, PoliticaVigente, DatosIngreso, DatosRegistro } from "./tipos";
