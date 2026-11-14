// Ruta /lotes/[loteId]/rotacion: página delgada que compone componentes de src/modulos.
// Exige sesión (RotacionConSesion manda a /ingresar si no la hay).
import { notFound } from "next/navigation";
import { obtenerPlanRotacion, RotacionConSesion } from "@/modulos/rotacion";

export default async function Page({ params }: { params: Promise<{ loteId: string }> }) {
  const { loteId } = await params;
  const plan = await obtenerPlanRotacion(loteId);
  if (!plan) notFound();
  return <RotacionConSesion plan={plan} />;
}
