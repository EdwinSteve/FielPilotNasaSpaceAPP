// Ruta /cultivos: la pestaña "Cultivos" cuando no hay un lote elegido. Abre la rotación del primer
// terreno del productor, o lo manda a registrar uno si aún no tiene (IrACultivos, exige sesión).
import { IrACultivos } from "@/modulos/rotacion";

export default function Page() {
  return <IrACultivos />;
}
