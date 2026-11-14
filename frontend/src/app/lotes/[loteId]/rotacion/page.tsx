// Ruta /lotes/[loteId]/rotacion: página delgada que compone componentes de src/modulos.
import { notFound } from "next/navigation";
import { obtenerPlanRotacion, PantallaRotacion } from "@/modulos/rotacion";

export default async function Page({ params }: { params: Promise<{ loteId: string }> }) {
  const { loteId } = await params;
  const plan = await obtenerPlanRotacion(loteId);
  if (!plan) notFound();
  return <PantallaRotacion plan={plan} />;
}
