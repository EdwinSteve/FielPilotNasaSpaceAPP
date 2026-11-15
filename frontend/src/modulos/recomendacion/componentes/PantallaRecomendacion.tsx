"use client";

// Recomendación del ciclo de cultivo de un terreno: el mejor cultivo y cuándo sembrarlo, el clima que
// se espera en los próximos meses (frente a lo normal) y el ranking de cultivos por fecha de siembra.
// Exige sesión. Gráficas de una sola serie en un tono (lluvia o verde), con el valor escrito junto a cada
// barra para no depender del color, y tooltip al pasar el cursor.
import Link from "next/link";
import { useCallback, useEffect, useState, type ReactNode } from "react";
import { aErrorApi, type ErrorApi } from "@/compartido/api/tipos";
import { CLASE_BOTON_SECUNDARIO, Spinner, TarjetaError } from "@/compartido/ui";
import { BarraSuperiorMovil } from "@/compartido/ui/BarraSuperiorMovil";
import { NavegacionPrincipal } from "@/compartido/ui/NavegacionPrincipal";
import { BotonCerrarSesion, SesionRequerida, type UsuarioSesion } from "@/modulos/identidad";
import { obtenerRecomendacion } from "../api";
import { deCadaDiez, fechaCorta, fechaLarga, fraseEnso, porcentaje } from "../formato";
import type { BloqueClima, CultivoRecomendado, PlanCiclo, RespuestaRecomendacion } from "../tipos";

const LOTE_ROTACION = "el-mirador";

function Marco({ usuario, children }: { usuario: UsuarioSesion; children: ReactNode }) {
  return (
    <div className="min-h-screen md:flex">
      <NavegacionPrincipal
        activa="terreno"
        usuario={usuario}
        loteId={LOTE_ROTACION}
        alertasPendientes={0}
        accionUsuario={<BotonCerrarSesion variante="lateral" />}
      />
      <main className="flex min-w-0 flex-1 flex-col gap-[14px] p-4 pb-[82px] md:gap-[18px] md:p-6 2xl:gap-6 2xl:px-10 2xl:py-8">
        <BarraSuperiorMovil iniciales={usuario.iniciales} accionUsuario={<BotonCerrarSesion variante="movil" />} />
        {children}
      </main>
    </div>
  );
}

function Tarjeta({ titulo, children, className = "" }: { titulo: string; children: ReactNode; className?: string }) {
  return (
    <section aria-label={titulo} className={`flex flex-col gap-4 rounded-2xl border border-borde bg-white p-4 md:p-5 ${className}`}>
      <h2 className="text-[17px] font-bold text-tinta">{titulo}</h2>
      {children}
    </section>
  );
}

function Dato({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <div className="flex flex-col">
      <span className="text-[12px] font-medium text-tinta-suave">{etiqueta}</span>
      <span className="text-[16px] font-bold text-tinta">{valor}</span>
    </div>
  );
}

function MejorCiclo({ plan, riego }: { plan: PlanCiclo; riego: boolean }) {
  if (!plan.mejor) {
    return (
      <Tarjeta titulo="Mejor ciclo">
        <p className="text-[14px] text-tinta-suave">Ningún cultivo del catálogo es apto para la altitud de este terreno.</p>
      </Tarjeta>
    );
  }
  const { siembra, cultivo: c } = plan.mejor;
  const rend = riego ? c.rend_con_riego_p50 : c.rend_p50;
  return (
    <section aria-label="Mejor ciclo" className="flex flex-col gap-4 rounded-2xl bg-verde-900 p-5 text-white md:p-6">
      <div className="flex flex-col gap-1">
        <span className="text-[13px] font-semibold tracking-wide text-verde-300 uppercase">Mejor ciclo para su terreno</span>
        <h2 className="text-[26px] font-bold md:text-[30px]">
          {c.nombre}: siembre el {fechaLarga(siembra)}
        </h2>
        <p className="text-[14px] text-white/80">
          Cosecha hacia el {fechaLarga(c.cosecha)} ({c.ciclo_dias} días). Es el cultivo con mejor rendimiento esperado entre las
          fechas de siembra del próximo mes.
        </p>
      </div>
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <DatoClaro etiqueta="Rendimiento esperado" valor={`${porcentaje(rend)} del potencial`} />
        <DatoClaro etiqueta="Rango probable" valor={`${porcentaje(c.rend_p10)} a ${porcentaje(c.rend_p90)}`} />
        <DatoClaro etiqueta="Le iría bien" valor={deCadaDiez(c.prob_exito)} />
        <DatoClaro etiqueta={riego ? "Riego necesario" : "Riego que ayudaría"} valor={`${Math.round(c.riego_mm_p50)} mm`} />
      </div>
    </section>
  );
}

function DatoClaro({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <div className="flex flex-col rounded-xl bg-white/10 p-3">
      <span className="text-[12px] text-white/70">{etiqueta}</span>
      <span className="text-[15px] font-bold">{valor}</span>
    </div>
  );
}

/** Barra 0 -> p50 (escenario típico), bigote p10-p90 y marca de lo normal, en la misma escala. */
function BarraRango({ p10, p50, p90, normal, maximo, color, titulo }: {
  p10: number; p50: number; p90: number; normal?: number; maximo: number; color: string; titulo: string;
}) {
  const x = (v: number) => `${Math.min(100, (v / maximo) * 100)}%`;
  return (
    <div className="relative h-5 w-full rounded-full bg-fondo" title={titulo}>
      <div className={`absolute top-1/2 h-[2px] -translate-y-1/2 ${color} opacity-40`} style={{ left: x(p10), width: `calc(${x(p90)} - ${x(p10)})` }} />
      <div className={`absolute top-1/2 left-0 h-3 -translate-y-1/2 rounded-r-[4px] ${color}`} style={{ width: x(p50) }} />
      {normal !== undefined && (
        <div className="absolute top-0 h-5 w-[2px] bg-tinta" style={{ left: x(normal) }} aria-hidden="true" />
      )}
    </div>
  );
}

function ClimaEsperado({ plan }: { plan: PlanCiclo }) {
  const enso = fraseEnso(plan.pronostico);
  const bloques = plan.pronostico.panorama;
  const maximo = Math.max(1, ...bloques.flatMap((b) => [b.lluvia_mm_p90, b.lluvia_normal_mm]));
  return (
    <Tarjeta titulo="Clima que se espera en su terreno">
      <div className="flex items-start gap-3 rounded-xl bg-amarilla-fondo p-3">
        <span className="rounded-md bg-white px-2 py-0.5 font-mono text-[12px] font-semibold text-tinta">ONI {plan.pronostico.oni.toFixed(1)}</span>
        <div>
          <p className="text-[14px] font-semibold text-tinta">{enso.titulo}</p>
          <p className="text-[13px] text-tinta-suave">{enso.detalle}</p>
        </div>
      </div>
      <p className="text-[13px] text-tinta-suave">
        Lluvia en cada periodo de 30 días: la barra es el escenario típico, la línea delgada va del año seco al húmedo y la
        marca negra es lo normal en su terreno.
      </p>
      <ul className="flex flex-col gap-3">
        {bloques.map((b) => (
          <FilaClima key={b.desde} b={b} maximo={maximo} />
        ))}
      </ul>
      <details className="text-[13px] text-tinta-suave">
        <summary className="cursor-pointer font-semibold text-verde-700">Ver como tabla</summary>
        <table className="mt-2 w-full text-left">
          <thead>
            <tr className="text-tinta">
              <th className="py-1 font-semibold">Periodo</th>
              <th className="py-1 font-semibold">Lluvia (seco · típico · húmedo)</th>
              <th className="py-1 font-semibold">Normal</th>
              <th className="py-1 font-semibold">Temp. máx.</th>
            </tr>
          </thead>
          <tbody>
            {bloques.map((b) => (
              <tr key={b.desde} className="border-t border-borde">
                <td className="py-1">{fechaCorta(b.desde)} – {fechaCorta(b.hasta)}</td>
                <td className="py-1">{b.lluvia_mm_p10} · {b.lluvia_mm_p50} · {b.lluvia_mm_p90} mm</td>
                <td className="py-1">{b.lluvia_normal_mm} mm</td>
                <td className="py-1">{b.tmax_c} °C (normal {b.tmax_normal_c} °C)</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </Tarjeta>
  );
}

function FilaClima({ b, maximo }: { b: BloqueClima; maximo: number }) {
  const anomalia = b.anomalia_lluvia_pct;
  const texto = anomalia <= -10 ? `${-anomalia} % menos que lo normal` : anomalia >= 10 ? `${anomalia} % más que lo normal` : "cerca de lo normal";
  return (
    <li className="grid grid-cols-[88px_minmax(0,1fr)] items-center gap-x-3 gap-y-1 md:grid-cols-[120px_minmax(0,1fr)_220px]">
      <span className="text-[13px] font-semibold text-tinta">
        {fechaCorta(b.desde)} – {fechaCorta(b.hasta)}
      </span>
      <BarraRango
        p10={b.lluvia_mm_p10}
        p50={b.lluvia_mm_p50}
        p90={b.lluvia_mm_p90}
        normal={b.lluvia_normal_mm}
        maximo={maximo}
        color="bg-lluvia"
        titulo={`Lluvia típica ${b.lluvia_mm_p50} mm (año seco ${b.lluvia_mm_p10}, húmedo ${b.lluvia_mm_p90}); normal ${b.lluvia_normal_mm} mm`}
      />
      <span className="col-start-2 text-[13px] text-tinta-suave md:col-start-3">
        <strong className="text-tinta">{b.lluvia_mm_p50} mm</strong> · {texto} · máx. {b.tmax_c} °C
      </span>
    </li>
  );
}

function FilaCultivo({ c, riego }: { c: CultivoRecomendado; riego: boolean }) {
  const rend = riego ? c.rend_con_riego_p50 : c.rend_p50;
  return (
    <li className="grid grid-cols-[minmax(0,1fr)] gap-1.5 border-t border-borde pt-3 first:border-t-0 first:pt-0 md:grid-cols-[160px_minmax(0,1fr)_220px] md:items-center md:gap-3">
      <span className="text-[15px] font-semibold text-tinta">{c.nombre}</span>
      <BarraRango
        p10={c.rend_p10}
        p50={rend}
        p90={c.rend_p90}
        maximo={1}
        color="bg-verde-700"
        titulo={`${c.nombre}: ${porcentaje(rend)} del potencial (entre ${porcentaje(c.rend_p10)} y ${porcentaje(c.rend_p90)})`}
      />
      <span className="text-[13px] text-tinta-suave">
        <strong className="text-tinta">{porcentaje(rend)}</strong> · le va bien {deCadaDiez(c.prob_exito)} · cosecha {fechaCorta(c.cosecha)}
      </span>
    </li>
  );
}

function RankingCultivos({ plan, riego }: { plan: PlanCiclo; riego: boolean }) {
  const [elegida, setElegida] = useState(plan.mejor?.siembra ?? plan.opciones[0]?.siembra);
  const opcion = plan.opciones.find((o) => o.siembra === elegida) ?? plan.opciones[0];
  if (!opcion) return null;
  const aptos = opcion.cultivos.filter((c) => c.apto);
  const noAptos = opcion.cultivos.filter((c) => !c.apto);
  return (
    <Tarjeta titulo="Cultivos para su terreno según la fecha de siembra">
      <div role="tablist" aria-label="Fecha de siembra" className="flex flex-wrap gap-2">
        {plan.opciones.map((o) => (
          <button
            key={o.siembra}
            role="tab"
            type="button"
            aria-selected={o.siembra === opcion.siembra}
            onClick={() => setElegida(o.siembra)}
            className={`min-h-10 rounded-full border px-4 text-[14px] font-semibold transition-colors ${
              o.siembra === opcion.siembra ? "border-verde-700 bg-verde-700 text-white" : "border-borde bg-white text-tinta hover:border-verde-500"
            }`}
          >
            Sembrar el {fechaCorta(o.siembra)}
            {o.siembra === plan.mejor?.siembra ? " ★" : ""}
          </button>
        ))}
      </div>
      <p className="text-[13px] text-tinta-suave">
        Rendimiento esperado como porcentaje del potencial de cada cultivo{riego ? ", con riego" : ""}. La barra es lo más probable y la
        línea delgada, el rango entre un año malo y uno bueno.
      </p>
      <ol className="flex flex-col gap-3">
        {aptos.slice(0, 8).map((c) => (
          <FilaCultivo key={c.cultivo} c={c} riego={riego} />
        ))}
      </ol>
      {noAptos.length > 0 && (
        <details className="text-[13px] text-tinta-suave">
          <summary className="cursor-pointer font-semibold text-verde-700">No recomendados para este terreno ({noAptos.length})</summary>
          <ul className="mt-2 flex flex-col gap-1">
            {noAptos.map((c) => (
              <li key={c.cultivo}>
                <strong className="text-tinta">{c.nombre}</strong>: {c.razon_no_apto}
              </li>
            ))}
          </ul>
        </details>
      )}
    </Tarjeta>
  );
}

function Cargando() {
  const [segundos, setSegundos] = useState(0);
  useEffect(() => {
    const t = setInterval(() => setSegundos((s) => s + 1), 1000);
    return () => clearInterval(t);
  }, []);
  return (
    <section role="status" className="flex flex-col items-start gap-3 rounded-2xl border border-borde bg-white p-5">
      <div className="flex items-center gap-3 text-[16px] font-semibold text-tinta">
        <Spinner className="h-5 w-5 text-verde-700" /> Calculando la recomendación de su terreno… {segundos} s
      </div>
      <p className="text-[14px] text-tinta-suave">
        Traemos el clima diario de su terreno desde 1981 (NASA POWER), el suelo (SoilGrids) y el estado de El Niño, y
        simulamos los cultivos con el pronóstico de los próximos meses. La primera vez puede tardar hasta 2 minutos.
      </p>
    </section>
  );
}

function Recomendacion({ usuario, terrenoId }: { usuario: UsuarioSesion; terrenoId: string }) {
  const [datos, setDatos] = useState<RespuestaRecomendacion | null>(null);
  const [error, setError] = useState<ErrorApi | null>(null);
  const cargar = useCallback(() => {
    setDatos(null);
    setError(null);
    obtenerRecomendacion(terrenoId)
      .then(setDatos)
      .catch((e) => setError(aErrorApi(e)));
  }, [terrenoId]);
  useEffect(cargar, [cargar]);

  const t = datos?.terreno;
  return (
    <Marco usuario={usuario}>
      <header className="flex flex-col gap-2 md:flex-row md:items-end md:justify-between">
        <div className="flex flex-col gap-[3px]">
          <h1 className="text-[22px] font-bold md:text-[24px] 2xl:text-[30px]">{t ? `Recomendación para ${t.nombre}` : "Recomendación de cultivo"}</h1>
          {datos && (
            <p className="text-[13px] text-tinta-suave md:text-[14px]">
              {t!.area_ha.toLocaleString("es-CO", { maximumFractionDigits: 2 })} ha · {Math.round(datos.plan.lote.elevacion_m)} m de
              altitud · pH del suelo {datos.plan.lote.ph_suelo.toFixed(1)} · {t!.riego ? "con riego" : "sin riego"}
            </p>
          )}
        </div>
        <Link href="/lotes" className={`${CLASE_BOTON_SECUNDARIO} w-auto`}>
          Mis terrenos
        </Link>
      </header>

      {!datos && !error && <Cargando />}
      {error && <TarjetaError mensaje={error.mensaje} codigo={error.codigo} alReintentar={cargar} />}
      {datos && (
        <>
          <MejorCiclo plan={datos.plan} riego={datos.terreno.riego} />
          <div className="grid gap-[14px] md:gap-[18px] 2xl:grid-cols-2 2xl:items-start">
            <ClimaEsperado plan={datos.plan} />
            <RankingCultivos plan={datos.plan} riego={datos.terreno.riego} />
          </div>
          <div className="grid grid-cols-2 gap-3 rounded-2xl border border-borde bg-white p-4 md:grid-cols-4">
            <Dato etiqueta="Pronóstico" valor={datos.plan.pronostico.fuente === "analogos" ? "Años análogos (ENSO)" : "SEAS5 (ECMWF)"} />
            <Dato etiqueta="Calculado" valor={fechaLarga(datos.plan.generado)} />
            <Dato etiqueta="Centro del terreno" valor={`${t!.lat.toFixed(4)}, ${t!.lon.toFixed(4)}`} />
            <Dato etiqueta="Modelo" valor={datos.plan.modelo} />
          </div>
        </>
      )}
    </Marco>
  );
}

export function RecomendacionConSesion({ terrenoId }: { terrenoId: string }) {
  return <SesionRequerida>{(usuario) => <Recomendacion usuario={usuario} terrenoId={terrenoId} />}</SesionRequerida>;
}
