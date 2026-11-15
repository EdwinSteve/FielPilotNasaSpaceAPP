import Link from "next/link";
import { Icono } from "@/compartido/ui/Icono";
import type { AlertaResumen, ClimaActual, UbicacionInicio } from "../tipos";
import { NIVELES, etiquetaDe } from "./niveles";

interface Props {
  ubicacion: UbicacionInicio;
  clima: ClimaActual;
  /** null: no hay alertas ni avisos; se muestra `sinAlertas`. */
  alerta: AlertaResumen | null;
  sinAlertas: string;
}

const CHIP = "rounded-full bg-white/16 px-[10px] py-[5px] text-[12px] font-medium text-white 2xl:px-3 2xl:py-[6px] 2xl:text-[13px]";
const TARJETA = "flex flex-col gap-[10px] rounded-2xl bg-white p-4 md:p-5 2xl:w-[540px] 2xl:justify-between 2xl:gap-6 2xl:p-6";
const TITULO = "text-[17px] leading-[1.45] font-semibold md:text-[20px] 2xl:text-[22px]";
const TEXTO = "text-[13px] leading-[1.45] text-tinta-suave md:text-[14px] 2xl:text-[15px]";

export function BannerClima({ ubicacion, clima, alerta, sinAlertas }: Props) {
  return (
    <section className="flex flex-col gap-4 rounded-3xl bg-linear-to-r from-[#14532d] to-[#0e7490] p-5 md:gap-5 md:p-7 2xl:flex-row 2xl:gap-8 2xl:p-8">
      {/* En escritorio es la columna izquierda; en tablet y móvil sus hijos se apilan con la alerta */}
      <div className="contents 2xl:flex 2xl:min-w-0 2xl:flex-1 2xl:flex-col 2xl:justify-between 2xl:gap-6">
        <div className="flex flex-wrap gap-[6px] 2xl:gap-2">
          <span className={CHIP}>
            {ubicacion.nombre}
            {ubicacion.departamento && `, ${ubicacion.departamento}`}
          </span>
          <span className={`${CHIP} hidden md:inline`}>{ubicacion.coordenadas}</span>
          {ubicacion.altitud && <span className={CHIP}>{ubicacion.altitud}</span>}
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

      {alerta ? (
        <article className={TARJETA}>
          <div className="flex flex-col gap-[10px] 2xl:gap-3">
            <span
              className={`flex items-center gap-2 self-start rounded-full px-3 py-[6px] text-[12px] font-bold ${NIVELES[alerta.nivel].fondo} ${NIVELES[alerta.nivel].texto}`}
            >
              <Icono nombre={NIVELES[alerta.nivel].iconoPequeno} tam={16} className="size-4" />
              {etiquetaDe(alerta)}
            </span>
            <h2 className={TITULO}>{alerta.titulo}</h2>
            <p className={TEXTO}>{alerta.detalle}</p>
          </div>
          <Link
            href="/alertas"
            className="rounded-[10px] bg-verde-700 px-4 py-3 text-center text-[14px] font-semibold text-white md:self-end md:py-[10px] 2xl:px-[18px]"
          >
            Ver qué hacer
          </Link>
        </article>
      ) : (
        <article className={`${TARJETA} 2xl:justify-center`}>
          <div className="flex flex-col gap-[10px] 2xl:gap-3">
            <span className="flex items-center gap-2 self-start rounded-full bg-verde-100 px-3 py-[6px] text-[12px] font-bold text-verde-700">
              <Icono nombre="check-circulo" tam={16} className="size-4" />
              SIN ALERTAS
            </span>
            <h2 className={TITULO}>Todo tranquilo en su zona</h2>
            <p className={TEXTO}>{sinAlertas}</p>
          </div>
        </article>
      )}
    </section>
  );
}
