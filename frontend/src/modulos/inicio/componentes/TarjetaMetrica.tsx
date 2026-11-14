import { Icono } from "@/compartido/ui/Icono";
import type { IconoMetrica, Metrica } from "../tipos";

const FONDO_ICONO: Record<IconoMetrica, string> = {
  thermo: "bg-[#fef3c7]",
  rain: "bg-[#e0f2fe]",
  drop: "bg-[#ccfbf1]",
  wind: "bg-[#e0e7ff]",
  sun: "bg-[#fef9c3]",
  layers: "bg-verde-100",
};

const FUENTE = "rounded-md bg-fondo py-[3px] font-semibold text-tinta-suave";

export function TarjetaMetrica({ metrica }: { metrica: Metrica }) {
  return (
    <article className="flex flex-col gap-2 rounded-2xl border border-borde bg-white p-[14px] md:gap-[10px] md:p-4 2xl:gap-[14px] 2xl:p-5">
      {/* En escritorio el nombre va junto al icono y la fuente al pie; en tablet y móvil, al revés */}
      <div className="flex items-center justify-between gap-[10px] 2xl:justify-start">
        <span className={`rounded-[10px] p-[6px] 2xl:p-2 ${FONDO_ICONO[metrica.icono]}`}>
          <Icono nombre={metrica.icono} />
        </span>
        <span className="hidden text-[14px] font-medium text-tinta-suave 2xl:block">
          {metrica.nombre}
        </span>
        <span className={`${FUENTE} px-[6px] text-[10px] 2xl:hidden`}>{metrica.fuente}</span>
      </div>
      <p className="text-[13px] leading-[1.45] font-medium text-tinta-suave 2xl:hidden">
        {metrica.nombre}
      </p>
      <p className="flex items-end gap-1 2xl:gap-[6px]">
        <span className="text-[26px] font-bold md:text-[30px] 2xl:text-[36px]">{metrica.valor}</span>
        <span className="text-[13px] font-medium text-tinta-suave 2xl:text-[16px]">
          {metrica.unidad}
        </span>
      </p>
      <p className="text-[12px] leading-[1.45] text-tinta-suave 2xl:text-[13px] 2xl:leading-[normal]">
        {metrica.detalle}
      </p>
      <span className={`${FUENTE} hidden self-start px-2 text-[11px] 2xl:block`}>
        {metrica.fuente}
      </span>
    </article>
  );
}
