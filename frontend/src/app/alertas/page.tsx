// Ruta /alertas: alertas de ríos y quebradas del IDEAM y el indicador de sequía de los lotes del productor.
// ?lote=<id> elige el lote (por defecto, el primero). Exige sesión (AlertasConSesion manda a /ingresar).
// Con los datos de ejemplo, ?simular=rios-503|rios-502|sequia-503|400 muestra los estados de error del diseño.
import { notFound } from "next/navigation";
import { AlertasConSesion, leerSimulacion, obtenerLotesUbicados } from "@/modulos/alertas";

export default async function Page({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const { lote, simular } = await searchParams;
  const lotes = await obtenerLotesUbicados();
  const actual = lote ? lotes.find((l) => l.id === lote) : lotes[0];
  if (!actual) notFound();
  return <AlertasConSesion lotes={lotes} actual={actual} simular={leerSimulacion(simular)} />;
}
