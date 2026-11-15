import Link from "next/link";
import type { ReactNode } from "react";
import { BarraSuperiorMovil } from "@/compartido/ui/BarraSuperiorMovil";
import { Icono } from "@/compartido/ui/Icono";
import type { ResumenInicio } from "../tipos";

export function Encabezado({ resumen, accionMovil }: { resumen: ResumenInicio; accionMovil?: ReactNode }) {
  const { usuario, saludo, fecha, ubicacion } = resumen;

  return (
    <header className="flex flex-col gap-[14px] md:flex-row md:items-center md:justify-between">
      <BarraSuperiorMovil iniciales={usuario.iniciales} accionUsuario={accionMovil} />

      <div className="flex flex-col gap-[3px] 2xl:gap-1">
        <h1 className="text-[22px] font-bold md:text-[24px] 2xl:text-[30px]">
          {saludo}, {usuario.nombre}
        </h1>
        <p className="text-[13px] text-tinta-suave 2xl:text-[15px]">
          <span className="md:hidden">{fecha.corta}</span>
          <span className="hidden md:inline 2xl:hidden">{fecha.media}</span>
          <span className="hidden 2xl:inline">{fecha.larga}</span>
          {" · "}
          {ubicacion.nombre}
          {ubicacion.departamento && <span className="hidden 2xl:inline"> ({ubicacion.departamento})</span>}
        </p>
      </div>

      <div className="flex items-center gap-[10px] 2xl:gap-3">
        {/* Clima de la cabecera municipal; cuando existan los terrenos del usuario, aquí va el selector de lote */}
        <div className="flex w-full items-center rounded-xl border border-borde bg-white px-[14px] py-[11px] md:w-auto md:px-3 md:py-[9px] 2xl:px-4 2xl:py-[10px]">
          <span className="flex items-center gap-2 text-[14px] font-semibold md:gap-[6px] md:text-[13px] 2xl:gap-2 2xl:text-[14px]">
            <Icono nombre="pin-oscuro" tam={18} className="size-4 2xl:size-[18px]" />
            {ubicacion.nombre}
          </span>
        </div>
        <Link
          href="/alertas"
          aria-label="Notificaciones"
          className="hidden rounded-xl border border-borde bg-white p-[9px] md:block 2xl:p-[11px]"
        >
          <Icono nombre="bell" className="size-[18px] 2xl:size-5" />
        </Link>
      </div>
    </header>
  );
}
