// Lo que se ve mientras el plan no está listo: buscándolo, calculándolo con el modelo (puede tardar
// minutos), sin terrenos, lote inexistente o error. Nunca una pantalla en blanco.
import Link from "next/link";
import type { ReactNode } from "react";
import { CLASE_BOTON_PRINCIPAL, Spinner, TarjetaError } from "@/compartido/ui";
import type { EstadoPlan } from "../hooks/usePlanRotacion";

function Marco({ children }: { children: ReactNode }) {
  return (
    <main className="flex min-h-dvh items-center justify-center bg-fondo p-4">
      <div className="flex w-full max-w-md flex-col gap-4">{children}</div>
    </main>
  );
}

function Tarjeta({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <section className="flex flex-col gap-3 rounded-2xl border border-borde bg-white p-5 text-left">
      <h1 className="text-[18px] font-bold">{titulo}</h1>
      {children}
    </section>
  );
}

const enlaceTerrenos = (
  <Link href="/lotes" className="text-center text-[14px] font-semibold text-verde-700">
    Volver a mis terrenos
  </Link>
);

export function EstadoRotacion({ estado, alReintentar }: { estado: Exclude<EstadoPlan, { fase: "listo" }>; alReintentar: () => void }) {
  switch (estado.fase) {
    case "cargando":
      return (
        <Marco>
          <p role="status" className="flex items-center justify-center gap-2 text-[15px] text-tinta-suave">
            <Spinner className="size-5 text-verde-700" />
            Buscando el plan de su lote…
          </p>
        </Marco>
      );
    case "calculando":
      return (
        <Marco>
          <Tarjeta titulo="Estamos calculando las mejores rotaciones para su lote">
            <p role="status" aria-live="polite" className="flex items-start gap-3 text-[15px] leading-[1.45]">
              <Spinner className="mt-[2px] size-5 shrink-0 text-verde-700" />
              <span>
                La primera vez revisamos más de 40 años de clima de su terreno, el suelo y el pronóstico de El Niño y
                La Niña. Puede tardar unos minutos: no cierre esta página.
              </span>
            </p>
          </Tarjeta>
        </Marco>
      );
    case "sin-lotes":
      return (
        <Marco>
          <Tarjeta titulo="Todavía no tiene terrenos">
            <p className="text-[15px] leading-[1.45] text-tinta-suave">
              Dibuje su terreno en el mapa y le diremos qué sembrar y en qué orden.
            </p>
            <Link href="/lotes/nuevo" className={CLASE_BOTON_PRINCIPAL}>
              Dibujar mi terreno
            </Link>
          </Tarjeta>
        </Marco>
      );
    case "no-encontrado":
      return (
        <Marco>
          <Tarjeta titulo="No encontramos ese lote">
            <p className="text-[15px] leading-[1.45] text-tinta-suave">
              Puede que lo haya borrado o que sea de otra cuenta.
            </p>
            <Link href="/lotes" className={CLASE_BOTON_PRINCIPAL}>
              Ver mis terrenos
            </Link>
          </Tarjeta>
        </Marco>
      );
    case "error":
      return (
        <Marco>
          <TarjetaError mensaje={estado.error.mensaje} codigo={estado.error.codigo} alReintentar={alReintentar} />
          {enlaceTerrenos}
        </Marco>
      );
  }
}
