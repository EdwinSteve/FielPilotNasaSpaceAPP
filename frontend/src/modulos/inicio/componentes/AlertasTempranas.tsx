import Link from "next/link";
import { Icono } from "@/compartido/ui/Icono";
import type { AlertaResumen } from "../tipos";
import { NIVELES } from "./niveles";

export function AlertasTempranas({ alertas }: { alertas: AlertaResumen[] }) {
  return (
    <section className="flex flex-col gap-3 rounded-2xl border border-borde bg-white p-4 md:p-5 2xl:gap-4 2xl:p-6">
      <div className="flex items-center justify-between font-semibold">
        <h2 className="text-[16px] 2xl:text-[18px]">Alertas tempranas IDEAM</h2>
        <Link href="/alertas" className="text-[13px] text-verde-700 2xl:text-[14px]">
          Ver todas
        </Link>
      </div>
      {alertas.map((a) => {
        const nivel = NIVELES[a.nivel];
        return (
          <article
            key={a.titulo}
            className="flex items-center gap-3 rounded-xl bg-fondo p-3 2xl:gap-[14px] 2xl:p-[14px]"
          >
            <span className={`rounded-[10px] p-2 2xl:p-[10px] ${nivel.fondo}`}>
              <Icono nombre={nivel.icono} />
            </span>
            <div className="flex min-w-0 flex-1 flex-col gap-[3px]">
              <p className={`text-[10px] font-bold 2xl:text-[11px] ${nivel.texto}`}>{nivel.etiqueta}</p>
              <h3 className="text-[14px] leading-[1.45] font-semibold 2xl:text-[15px] 2xl:leading-[normal]">
                {a.titulo}
              </h3>
              <p className="text-[12px] leading-[1.45] text-tinta-suave 2xl:text-[13px]">{a.detalle}</p>
            </div>
          </article>
        );
      })}
      <p className="text-[11px] leading-[1.45] text-tinta-suave 2xl:text-[12px]">
        Fuente: boletín de alertas del IDEAM. Niveles: amarilla, naranja y roja.
      </p>
    </section>
  );
}
