// Ruta /alertas: alertas de ríos y quebradas del IDEAM y el indicador de sequía de los lotes del productor.
// ?lote=<id> elige el lote (por defecto, el primero). Exige sesión: los terrenos y las alertas se piden
// en el navegador con el token (AlertasConSesion), y sin sesión manda a /ingresar.
// ?simular=rios-503|rios-502|sequia-503|400 fuerza los estados de error del diseño.
import { AlertasConSesion, leerSimulacion } from "@/modulos/alertas";

export default async function Page({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const { lote, simular } = await searchParams;
  return <AlertasConSesion loteId={typeof lote === "string" ? lote : undefined} simular={leerSimulacion(simular)} />;
}
