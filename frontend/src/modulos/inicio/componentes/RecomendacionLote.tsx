import Link from "next/link";
import { Icono } from "@/compartido/ui/Icono";
import type { RecomendacionLote as Recomendacion } from "../tipos";

interface Props {
  /** null: todavía no hay recomendación para el terreno del usuario. */
  recomendacion: Recomendacion | null;
  rutaRotacion: string;
}

export function RecomendacionLote({ recomendacion, rutaRotacion }: Props) {
  return (
    <section className="flex flex-col justify-between gap-5 rounded-2xl bg-verde-900 p-5 2xl:p-6">
      <div className="flex flex-col gap-3 2xl:gap-[14px]">
        <p className="flex items-center gap-2 text-[11px] font-bold text-verde-300 2xl:text-[12px]">
          <Icono nombre="sprout-verde" className="size-[18px] 2xl:size-5" />
          RECOMENDACIÓN PARA TU LOTE
        </p>
        <h2 className="text-[19px] leading-[1.45] font-semibold text-white 2xl:text-[22px]">
          {recomendacion?.titulo ?? "Qué sembrar y cuándo"}
        </h2>
        <p className="text-[13px] leading-[1.45] text-[#c7dace] 2xl:text-[14px]">
          {recomendacion?.descripcion ??
            "Revise el plan de rotación: los cultivos y el orden que mejor le pueden resultar con el clima esperado."}
        </p>
        {recomendacion && (
          <div className="flex flex-col gap-[5px] rounded-xl bg-white/8 p-3 2xl:gap-[6px] 2xl:p-[14px]">
            <p className="text-[11px] font-medium text-verde-niebla 2xl:text-[12px]">Rotación sugerida</p>
            <p className="text-[15px] font-semibold text-white 2xl:text-[16px]">{recomendacion.rotacion}</p>
          </div>
        )}
      </div>
      <Link
        href={rutaRotacion}
        className="rounded-[10px] bg-white py-3 text-center text-[14px] font-semibold text-verde-900"
      >
        Ver plan de rotación
      </Link>
    </section>
  );
}
