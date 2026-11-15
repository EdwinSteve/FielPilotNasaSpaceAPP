// Ruta /lotes/[loteId]: la recomendación del ciclo de cultivo del terreno (el productor llega aquí al
// guardar el terreno). Página delgada que compone componentes de src/modulos.
import { RecomendacionConSesion } from "@/modulos/recomendacion";

export default async function Page({ params }: { params: Promise<{ loteId: string }> }) {
  const { loteId } = await params;
  return <RecomendacionConSesion terrenoId={loteId} />;
}
