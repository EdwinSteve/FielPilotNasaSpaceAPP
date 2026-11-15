// Ruta /lotes/[loteId]/rotacion: página delgada que compone componentes de src/modulos.
// Exige sesión. El plan sale del backend con el token del navegador, así que se pide en el cliente
// (RotacionConSesion): mientras tanto muestra en qué va, y "no encontrado" si el lote no es suyo.
import { RotacionConSesion } from "@/modulos/rotacion";

export default async function Page({ params }: { params: Promise<{ loteId: string }> }) {
  const { loteId } = await params;
  return <RotacionConSesion loteId={loteId} />;
}
