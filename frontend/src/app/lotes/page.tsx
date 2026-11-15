// Ruta /lotes ("Mi terreno"): página delgada que compone componentes de src/modulos.
// Suspense: la pantalla lee ?nuevo=1 con useSearchParams.
import { Suspense } from "react";
import { TerrenosConSesion } from "@/modulos/fincas";

export default function Page() {
  return (
    <Suspense>
      <TerrenosConSesion />
    </Suspense>
  );
}
