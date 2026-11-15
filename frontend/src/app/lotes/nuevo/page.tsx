// Ruta /lotes/nuevo: el productor dibuja su terreno en el mapa. Justo después del registro llega aquí
// con ?bienvenida=1.
import { Suspense } from "react";
import { NuevoTerrenoConSesion } from "@/modulos/fincas";

export default function Page() {
  return (
    <Suspense>
      <NuevoTerrenoConSesion />
    </Suspense>
  );
}
