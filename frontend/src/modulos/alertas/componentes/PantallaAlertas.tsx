// Pantalla Alertas IDEAM: alerta de ríos y quebradas del IDEAM y el indicador de sequía (NASA POWER)
// del lote elegido, la guía de niveles y el resumen de todos los lotes. En escritorio, dos columnas
// (ríos y guía a la izquierda; sequía y terrenos a la derecha); en tablet y móvil, una sola.
"use client";

import type { ReactNode } from "react";
import { Spinner } from "@/compartido/ui";
import { BarraSuperiorMovil } from "@/compartido/ui/BarraSuperiorMovil";
import { Icono } from "@/compartido/ui/Icono";
import { NavegacionPrincipal } from "@/compartido/ui/NavegacionPrincipal";
import { SelectorLote } from "@/modulos/rotacion";
import { tieneAlerta } from "../respuestas";
import type { AlertasLote, LoteUbicado } from "../tipos";
import { SignificadoNiveles } from "./SignificadoNiveles";
import { TarjetaRios } from "./TarjetaRios";
import { TarjetaSequia } from "./TarjetaSequia";
import { TusTerrenos } from "./TusTerrenos";

const INTRO =
  "Alertas de ríos y quebradas del IDEAM y un indicador de sequía con datos de la NASA, para la ubicación de tu terreno.";
const INTRO_CORTA = "Ríos y quebradas (IDEAM) y sequía (NASA) para tu terreno.";

const enAlertas = (id: string) => `/alertas?lote=${id}`;

export interface AccionesUsuario {
  lateral?: ReactNode;
  movil?: ReactNode;
}

interface Props {
  usuario: { nombreCompleto: string; iniciales: string; rol: string; municipio: string };
  lotes: LoteUbicado[];
  actual: LoteUbicado;
  alertasDe: (id: string) => AlertasLote;
  actualizando: boolean;
  alActualizar: () => void;
  alReintentar: (lote: LoteUbicado) => void;
  acciones?: AccionesUsuario;
}

export function PantallaAlertas({
  usuario,
  lotes,
  actual,
  alertasDe,
  actualizando,
  alActualizar,
  alReintentar,
  acciones,
}: Props) {
  const alertas = alertasDe(actual.id);
  const pendientes = lotes.filter((l) => tieneAlerta(alertasDe(l.id))).length;

  return (
    <div className="min-h-screen md:flex">
      <NavegacionPrincipal
        activa="alertas"
        usuario={usuario}
        loteId={actual.id}
        alertasPendientes={pendientes}
        accionUsuario={acciones?.lateral}
      />
      <main className="flex min-w-0 flex-1 flex-col gap-[14px] p-4 pb-[82px] md:gap-[18px] md:p-6 2xl:gap-5 2xl:px-10 2xl:py-8">
        <header className="flex flex-col gap-[14px] md:flex-row md:items-center md:justify-between">
          <BarraSuperiorMovil iniciales={usuario.iniciales} accionUsuario={acciones?.movil} />
          <div className="flex flex-col gap-[3px] 2xl:gap-1">
            <h1 className="text-[22px] font-bold md:text-[24px] 2xl:text-[30px]">Alertas IDEAM</h1>
            <p className="text-[13px] leading-[1.45] text-tinta-suave md:leading-normal 2xl:text-[15px]">
              <span className="md:hidden">{INTRO_CORTA}</span>
              <span className="hidden md:inline">{INTRO}</span>
            </p>
          </div>
          <div className="flex items-stretch gap-2 md:items-center 2xl:gap-3">
            <div className="min-w-0 flex-1 md:flex-none md:whitespace-nowrap">
              <SelectorLote lotes={lotes} actual={actual} destino={enAlertas} />
            </div>
            <button
              type="button"
              onClick={alActualizar}
              disabled={actualizando}
              aria-label="Actualizar alertas"
              className="flex shrink-0 items-center gap-2 rounded-[10px] bg-verde-700 p-3 text-[14px] font-semibold text-white transition-colors hover:bg-verde-900 disabled:opacity-70 md:px-4 md:py-[10px]"
            >
              {actualizando ? (
                <Spinner className="size-4" />
              ) : (
                <Icono nombre="rotate-blanco-16" tam={16} />
              )}
              <span className="hidden md:inline">{actualizando ? "Actualizando…" : "Actualizar"}</span>
            </button>
          </div>
        </header>

        <div className="grid gap-[14px] md:gap-[18px] 2xl:grid-cols-[minmax(0,1fr)_500px] 2xl:gap-5">
          <TarjetaRios consulta={alertas.rios} alReintentar={() => alReintentar(actual)} />
          <TarjetaSequia consulta={alertas.sequia} alReintentar={() => alReintentar(actual)} />
          <div className="2xl:col-start-2 2xl:row-start-2">
            <TusTerrenos lotes={lotes} actualId={actual.id} alertasDe={alertasDe} />
          </div>
          <div className="2xl:col-start-1 2xl:row-start-2">
            <SignificadoNiveles />
          </div>
        </div>
      </main>
    </div>
  );
}
