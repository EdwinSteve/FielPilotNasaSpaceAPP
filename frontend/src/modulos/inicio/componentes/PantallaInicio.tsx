// Pantalla de inicio del agricultor: clima del lote, métricas, pronóstico, alertas y recomendación.
import type { ReactNode } from "react";
import { NavegacionPrincipal } from "@/compartido/ui/NavegacionPrincipal";
import type { ResumenInicio } from "../tipos";
import { AlertasTempranas } from "./AlertasTempranas";
import { BannerClima } from "./BannerClima";
import { Encabezado } from "./Encabezado";
import { Pronostico } from "./Pronostico";
import { RecomendacionLote } from "./RecomendacionLote";
import { TarjetaMetrica } from "./TarjetaMetrica";

/** Acciones del usuario (cerrar sesión): en la barra lateral y en la barra superior del celular. */
export interface AccionesUsuario {
  lateral?: ReactNode;
  movil?: ReactNode;
}

export function PantallaInicio({ resumen, acciones }: { resumen: ResumenInicio; acciones?: AccionesUsuario }) {
  const rutaRotacion = `/lotes/${resumen.loteId}/rotacion`;

  return (
    <div className="min-h-screen md:flex">
      <NavegacionPrincipal
        activa="inicio"
        usuario={resumen.usuario}
        loteId={resumen.loteId}
        alertasPendientes={resumen.alertasPendientes}
        accionUsuario={acciones?.lateral}
      />
      <main className="flex min-w-0 flex-1 flex-col gap-[14px] p-4 pb-[82px] md:gap-[18px] md:p-6 2xl:min-h-screen 2xl:gap-6 2xl:px-10 2xl:py-8">
        <Encabezado resumen={resumen} accionMovil={acciones?.movil} />
        <BannerClima
          ubicacion={resumen.ubicacion}
          clima={resumen.clima}
          alerta={resumen.alertaDestacada}
          sinAlertas={resumen.sinAlertas}
        />

        <h2 className="text-[16px] font-semibold md:hidden">Métricas del terreno</h2>
        <section
          aria-label="Métricas del terreno"
          className="grid grid-cols-2 gap-[10px] md:grid-cols-3 md:gap-[14px] 2xl:grid-cols-6 2xl:gap-4"
        >
          {resumen.metricas.map((m) => (
            <TarjetaMetrica key={m.nombre} metrica={m} />
          ))}
        </section>

        <div className="grid gap-[14px] md:grid-cols-2 md:gap-y-[18px] 2xl:flex-1 2xl:grid-cols-[minmax(0,748fr)_minmax(0,480fr)_minmax(0,380fr)] 2xl:gap-6">
          <Pronostico dias={resumen.pronostico} />
          <AlertasTempranas alertas={resumen.alertas} />
          <RecomendacionLote recomendacion={resumen.recomendacion} rutaRotacion={rutaRotacion} />
        </div>
      </main>
    </div>
  );
}
