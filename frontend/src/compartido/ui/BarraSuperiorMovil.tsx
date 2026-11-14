// Barra superior de las pantallas en móvil, donde no hay barra lateral: marca, notificaciones y avatar.
import Link from "next/link";
import type { ReactNode } from "react";
import { Icono } from "./Icono";

export function BarraSuperiorMovil({ iniciales, accionUsuario }: { iniciales: string; accionUsuario?: ReactNode }) {
  return (
    <div className="flex items-center justify-between md:hidden">
      <div className="flex items-center gap-2">
        <span className="rounded-lg bg-verde-500 p-[6px]">
          <Icono nombre="sprout-blanco" className="size-4" />
        </span>
        <span className="text-[16px] font-bold">Field Shift</span>
      </div>
      <div className="flex items-center gap-2">
        <Link href="/alertas" aria-label="Notificaciones" className="rounded-xl border border-borde bg-white p-2">
          <Icono nombre="bell" className="size-[18px]" />
        </Link>
        <span className="flex size-9 items-center justify-center rounded-full bg-verde-500 text-[12px] font-bold text-white">
          {iniciales}
        </span>
        {accionUsuario}
      </div>
    </div>
  );
}
