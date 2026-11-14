// Ruta /inicio: página delgada que compone componentes de src/modulos.
import { obtenerResumenInicio, PantallaInicio } from "@/modulos/inicio";

export default async function Page() {
  const resumen = await obtenerResumenInicio();
  return <PantallaInicio resumen={resumen} />;
}
