import Link from "next/link";
import { Icono } from "@/compartido/ui/Icono";
import type { AlertaDestacada, ClimaActual, LoteInicio } from "../tipos";
import { NIVELES } from "./niveles";

interface Props {
  lote: LoteInicio;
  clima: ClimaActual;
  alerta: AlertaDestacada;
}

const CHIP = "rounded-full bg-white/16 px-[10px] py-[5px] text-[12px] font-medium text-white 2xl:px-3 2xl:py-[6px] 2xl:text-[13px]";

export function BannerClima({ lote, clima, alerta }: Props) {
  const nivel = NIVELES[alerta.nivel];

  return (
    <section className="flex flex-col gap-4 rounded-3xl bg-linear-to-r from-[#14532d] to-[#0e7490] p-5 md:gap-5 md:p-7 2xl:flex-row 2xl:gap-8 2xl:p-8">
      {/* En escritorio es la columna izquierda; en tablet y móvil sus hijos se apilan con la alerta */}
      <div className="contents 2xl:flex 2xl:min-w-0 2xl:flex-1 2xl:flex-col 2xl:justify-between 2xl:gap-6">
        <div className="flex flex-wrap gap-[6px] 2xl:gap-2">
          <span className={CHIP}>
            <span className="hidden md:inline">Lote </span>
            {lote.nombre} · {lote.area}
          </span>
          <span className={`${CHIP} hidden md:inline`}>{lote.coordenadas}</span>
          <span className={CHIP}>{lote.altitud}</span>
        </div>

        <div className="flex flex-wrap items-center gap-4 md:flex-nowrap md:gap-5">
          <Icono nombre="cloudsun-blanco" tam={80} className="size-11 md:size-[68px] 2xl:size-20" />
          <p className="text-[64px] font-bold text-white md:text-[84px] 2xl:text-[96px]">
            {clima.temperatura}
          </p>
          <div className="flex w-full flex-col gap-1 md:w-auto md:min-w-0 md:flex-1 2xl:gap-[6px]">
            <p className="text-[18px] leading-[1.45] font-semibold text-white md:text-[24px] 2xl:text-[26px] 2xl:leading-[normal]">
              {clima.condicion}
            </p>
            <p className="text-[13px] leading-[1.45] text-[#d7ebe0] md:text-[15px] 2xl:text-[16px] 2xl:leading-[normal]">
              {clima.detalle}
            </p>
            <p className="text-[13px] leading-[1.45] text-[#d7ebe0] md:text-[15px] 2xl:text-[16px] 2xl:leading-[normal]">
              {clima.lluviaHoy}
            </p>
          </div>
        </div>

        <p className="order-last text-[13px] text-[#bfdccd] 2xl:order-none">{clima.fuentes}</p>
      </div>

      <article className="flex flex-col gap-[10px] rounded-2xl bg-white p-4 md:p-5 2xl:w-[540px] 2xl:justify-between 2xl:gap-6 2xl:p-6">
        <div className="flex flex-col gap-[10px] 2xl:gap-3">
          <span
            className={`flex items-center gap-2 self-start rounded-full px-3 py-[6px] text-[12px] font-bold ${nivel.fondo} ${nivel.texto}`}
          >
            <Icono nombre={nivel.iconoPequeno} tam={16} className="size-4" />
            {nivel.etiqueta} · IDEAM
          </span>
          <h2 className="text-[17px] leading-[1.45] font-semibold md:text-[20px] 2xl:text-[22px]">
            {alerta.titulo}
          </h2>
          <p className="text-[13px] leading-[1.45] text-tinta-suave md:text-[14px] 2xl:text-[15px]">
            {alerta.descripcion}
          </p>
        </div>
        <div className="flex flex-col gap-[10px] md:flex-row md:items-center md:justify-between">
          <p className="text-[13px] font-medium text-tinta-suave">{alerta.vigencia}</p>
          <Link
            href="/alertas"
            className="rounded-[10px] bg-verde-700 px-4 py-3 text-center text-[14px] font-semibold text-white md:py-[10px] 2xl:px-[18px]"
          >
            Ver qué hacer
          </Link>
        </div>
      </article>
    </section>
  );
}
