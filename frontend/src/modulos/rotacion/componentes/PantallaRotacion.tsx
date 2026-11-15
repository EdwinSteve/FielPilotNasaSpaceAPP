// Pantalla Rotación de cultivos (plan de siembra): las 3 mejores rotaciones, el ranking de
// cultivos y las lluvias esperadas del lote. En escritorio, dos columnas (rotaciones y lluvias
// a la izquierda, ranking a la derecha); en tablet y móvil, una sola.
import Link from "next/link";
import type { ReactNode } from "react";
import { BarraSuperiorMovil } from "@/compartido/ui/BarraSuperiorMovil";
import { Icono } from "@/compartido/ui/Icono";
import { NavegacionPrincipal } from "@/compartido/ui/NavegacionPrincipal";
import type { PlanRotacion } from "../tipos";
import { LluviasEsperadas } from "./LluviasEsperadas";
import { MejoresRotaciones } from "./MejoresRotaciones";
import { RankingCultivos } from "./RankingCultivos";
import type { AccionesSeleccion } from "./MejoresRotaciones";
import { SelectorLote } from "./SelectorLote";

const INTRO = "Analizamos 40 años de clima de su zona y el pronóstico de El Niño y La Niña.";

/** Acciones del usuario (cerrar sesión): en la barra lateral y en la barra superior del celular. */
export interface AccionesUsuario {
  lateral?: ReactNode;
  movil?: ReactNode;
}

interface Props {
  plan: PlanRotacion;
  acciones?: AccionesUsuario;
  /** Elegir un ciclo (sin esto las tarjetas no muestran el botón, como en los datos de ejemplo). */
  seleccion?: AccionesSeleccion;
}

export function PantallaRotacion({ plan, acciones, seleccion }: Props) {
  return (
    <div className="min-h-screen md:flex">
      <NavegacionPrincipal
        activa="rotacion"
        usuario={plan.usuario}
        loteId={plan.lote.id}
        alertasPendientes={plan.alertasPendientes}
        accionUsuario={acciones?.lateral}
      />
      <main className="flex min-w-0 flex-1 flex-col gap-[14px] p-4 pb-[82px] md:gap-[18px] md:p-6 2xl:gap-6 2xl:px-10 2xl:py-8">
        <header className="flex flex-col gap-[14px] md:flex-row md:items-center md:justify-between">
          <BarraSuperiorMovil iniciales={plan.usuario.iniciales} accionUsuario={acciones?.movil} />
          <div className="flex flex-col gap-[3px] 2xl:gap-1">
            <h1 className="text-[22px] font-bold md:text-[24px] 2xl:text-[30px]">Plan de siembra</h1>
            <p className="text-[13px] leading-[18px] text-tinta-suave md:leading-normal 2xl:text-[15px]">
              <span className="md:hidden">{INTRO}</span>
              <span className="hidden md:inline 2xl:hidden">
                Rotación de cultivos · {plan.finca.nombre}, {plan.finca.municipio}
              </span>
              <span className="hidden 2xl:inline">Rotación de cultivos · {INTRO}</span>
            </p>
          </div>
          <div className="flex items-center gap-[10px] 2xl:gap-3">
            <SelectorLote lotes={plan.lotes} actual={plan.lote} />
            <Link
              href="/alertas"
              aria-label="Notificaciones"
              className="hidden rounded-xl border border-borde bg-white p-[9px] md:block 2xl:p-[11px]"
            >
              <Icono nombre="bell" className="size-[18px] 2xl:size-5" />
            </Link>
          </div>
        </header>
        <p className="hidden text-[13px] text-tinta-suave md:block 2xl:hidden">{INTRO}</p>

        <div className="grid gap-[14px] md:gap-[18px] 2xl:grid-cols-[minmax(0,1fr)_620px] 2xl:grid-rows-[auto_1fr] 2xl:items-start 2xl:gap-6">
          <MejoresRotaciones rotaciones={plan.rotaciones} elegida={plan.seleccion?.rotacionId ?? null} acciones={seleccion} />
          <RankingCultivos ranking={plan.ranking} />
          <LluviasEsperadas lluvias={plan.lluvias} />
        </div>
      </main>
    </div>
  );
}
