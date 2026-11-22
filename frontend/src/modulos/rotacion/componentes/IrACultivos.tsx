"use client";

// Pestaña "Cultivos" sin un lote elegido: abre la rotación del primer terreno del productor.
// Si todavía no tiene terrenos, lo manda a registrar uno. Exige sesión.
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { aErrorApi, type ErrorApi } from "@/compartido/api/tipos";
import { TarjetaError } from "@/compartido/ui";
import { rutaCultivos } from "@/compartido/lib/rutas";
import { listarTerrenos } from "@/modulos/fincas";
import { SesionRequerida } from "@/modulos/identidad";

function BuscarPrimerTerreno() {
  const router = useRouter();
  const [error, setError] = useState<ErrorApi | null>(null);

  const buscar = useCallback(() => {
    setError(null);
    listarTerrenos()
      .then((terrenos) => router.replace(terrenos[0] ? rutaCultivos(terrenos[0].id) : "/lotes/nuevo"))
      .catch((e) => setError(aErrorApi(e)));
  }, [router]);
  useEffect(buscar, [buscar]);

  return (
    <main className="flex min-h-dvh items-center justify-center bg-fondo p-4">
      <div className="w-full max-w-md">
        {error ? (
          <TarjetaError mensaje={error.mensaje} codigo={error.codigo} alReintentar={buscar} />
        ) : (
          <p role="status" className="text-center text-[15px] text-tinta-suave">
            Buscando su terreno…
          </p>
        )}
      </div>
    </main>
  );
}

export function IrACultivos() {
  return <SesionRequerida>{() => <BuscarPrimerTerreno />}</SesionRequerida>;
}
