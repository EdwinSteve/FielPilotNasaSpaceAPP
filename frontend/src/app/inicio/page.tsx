// Ruta /inicio: página delgada que compone componentes de src/modulos.
// Exige sesión (InicioConSesion manda a /ingresar si no la hay).
import { InicioConSesion, obtenerResumenInicio } from "@/modulos/inicio";

export default async function Page() {
  const resumen = await obtenerResumenInicio();
  return <InicioConSesion resumen={resumen} />;
}
