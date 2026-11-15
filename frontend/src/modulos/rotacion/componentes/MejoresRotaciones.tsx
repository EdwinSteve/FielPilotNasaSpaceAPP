// Las 3 mejores rotaciones del lote. Cada tarjeta se abre y se cierra; la primera llega abierta (o la
// elegida, si ya hay una). "Elegir este ciclo" la guarda como plan activo del lote.
"use client";

import { Fragment, useId, useState } from "react";
import { Icono } from "@/compartido/ui/Icono";
import type { Rotacion } from "../tipos";

const textoFrecuencia = (n: number) => `${n} de cada 10 años le va bien`;

const fecha = new Intl.DateTimeFormat("es-CO", { day: "numeric", month: "short", year: "numeric", timeZone: "UTC" });
/** "2026-11-16" → "16 nov 2026" */
const formatearFecha = (iso: string) => fecha.format(new Date(`${iso}T00:00:00Z`)).replace(/\./g, "");

/** Elegir un ciclo: la pantalla con datos reales lo pasa; sin esto no se muestra el botón. */
export interface AccionesSeleccion {
  alElegir: (rotacionId: string) => void;
  /** La rotación que se está guardando ahora. */
  eligiendo: string | null;
  /** Por qué no se pudo guardar la última elección. */
  error: string | null;
}

function Etiqueta({ children, destacada = false }: { children: React.ReactNode; destacada?: boolean }) {
  return (
    <span
      className={`inline-flex items-center gap-[5px] rounded-full px-[10px] py-1 text-[12px] font-semibold ${
        destacada ? "bg-verde-100 text-verde-700" : "bg-fondo"
      }`}
    >
      {children}
    </span>
  );
}

interface TarjetaProps {
  rotacion: Rotacion;
  posicion: number;
  abierta: boolean;
  onAlternar: () => void;
  elegida: boolean;
  acciones?: AccionesSeleccion;
}

function TarjetaRotacion({ rotacion, posicion, abierta, onAlternar, elegida, acciones }: TarjetaProps) {
  const idPanel = useId();
  const mejor = posicion === 1;
  const secuencia = rotacion.secuencia.join(" → ");

  return (
    <li
      className={`flex flex-col gap-4 rounded-2xl bg-white p-4 md:p-5 2xl:p-6 ${
        mejor ? "border-2 border-verde-700" : "border border-borde"
      }`}
    >
      <h3>
        <button
          type="button"
          onClick={onAlternar}
          aria-expanded={abierta}
          aria-controls={idPanel}
          className={`flex w-full gap-3 text-left ${abierta ? "items-center" : "items-start"}`}
        >
          <span
            aria-hidden
            className={`flex size-10 shrink-0 items-center justify-center rounded-full text-[16px] font-bold ${
              mejor ? "bg-verde-700 text-white" : "bg-borde"
            }`}
          >
            {posicion}
          </span>
          <span className="flex min-w-0 flex-1 flex-col gap-[6px]">
            <span className="sr-only">Opción {posicion}: </span>
            <span className="flex flex-wrap items-center gap-2">
              {mejor && (
                <Etiqueta destacada>
                  <Icono nombre="insignia" tam={14} />
                  Mejor opción
                </Etiqueta>
              )}
              <Etiqueta>Grado {rotacion.grado}</Etiqueta>
              {elegida && (
                <Etiqueta destacada>
                  <Icono nombre="check" tam={14} />
                  Su ciclo elegido
                </Etiqueta>
              )}
            </span>
            {abierta ? (
              <span className="sr-only">{secuencia}</span>
            ) : (
              <>
                <span className="text-[16px] font-semibold">{secuencia}</span>
                <span className="text-[13px] text-tinta-suave">{textoFrecuencia(rotacion.frecuenciaExito)}</span>
              </>
            )}
          </span>
          <Icono nombre={abierta ? "chevron-arriba" : "chevron-abajo"} className={abierta ? "" : "mt-[10px]"} />
        </button>
      </h3>

      <div id={idPanel} hidden={!abierta} className="flex flex-col gap-4">
        <ol aria-label={`Orden de siembra: ${rotacion.secuencia.join(", luego ")}`} className="flex flex-wrap items-center gap-2">
          {rotacion.secuencia.map((cultivo, i) => (
            <Fragment key={`${cultivo}-${i}`}>
              {i > 0 && (
                <li aria-hidden>
                  <Icono nombre="flecha" tam={18} />
                </li>
              )}
              <li className="rounded-[10px] bg-verde-100 px-3 py-2 text-[16px] font-bold text-verde-700 md:px-4 md:text-[18px]">
                {cultivo}
              </li>
            </Fragment>
          ))}
        </ol>

        <p className="flex items-center gap-[10px] rounded-xl bg-fondo p-[14px] text-[16px] font-semibold">
          <Icono nombre="check-circulo" tam={22} />
          {textoFrecuencia(rotacion.frecuenciaExito)}
        </p>

        <dl className="grid gap-3 md:grid-cols-2">
          <div className="flex gap-3 rounded-xl border border-borde p-[14px]">
            <Icono nombre="gota-agua" />
            <div className="flex flex-col gap-1">
              <dt className="text-[12px] font-medium text-tinta-suave">Agua que se ahorra</dt>
              <dd className="text-[20px] font-bold">{rotacion.aguaAhorrada}</dd>
            </div>
          </div>
          {rotacion.aporteNitrogeno && (
            <div className="flex gap-3 rounded-xl border border-borde p-[14px]">
              <Icono nombre="brote" />
              <div className="flex flex-col gap-1">
                <dt className="text-[12px] font-medium text-tinta-suave">Abono natural para el suelo</dt>
                <dd className="text-[14px] leading-5 font-semibold">{rotacion.aporteNitrogeno}</dd>
              </div>
            </div>
          )}
        </dl>

        {rotacion.temporadas.length > 0 && (
          <ol aria-label="Cuándo sembrar y cosechar" className="flex flex-col gap-1 text-[13px] text-tinta-suave">
            {rotacion.temporadas.map((t) => (
              <li key={`${t.cultivo}-${t.siembra}`}>
                <span className="font-semibold text-tinta">{t.nombre}</span>: siembra {formatearFecha(t.siembra)},
                cosecha {formatearFecha(t.cosecha)}
                {t.fuente === "pronostico" ? " · con el pronóstico de los próximos meses" : " · según años anteriores"}
              </li>
            ))}
          </ol>
        )}

        {acciones &&
          (elegida ? (
            <p
              role="status"
              className="flex items-center justify-center gap-2 rounded-[10px] bg-verde-100 px-4 py-3 text-[14px] font-semibold text-verde-700"
            >
              <Icono nombre="check" tam={16} />
              {acciones.eligiendo === rotacion.id ? "Guardando su elección…" : "Este es su ciclo elegido"}
            </p>
          ) : (
            <button
              type="button"
              onClick={() => acciones.alElegir(rotacion.id)}
              disabled={acciones.eligiendo !== null}
              className="flex items-center justify-center gap-2 rounded-[10px] bg-verde-700 px-4 py-3 text-[14px] font-semibold text-white transition-colors hover:bg-verde-900 disabled:opacity-60"
            >
              Elegir este ciclo
              <Icono nombre="flecha-blanca" tam={16} />
            </button>
          ))}
      </div>
    </li>
  );
}

interface Props {
  rotaciones: Rotacion[];
  /** La rotación que el agricultor eligió; null si todavía no elige. */
  elegida?: string | null;
  acciones?: AccionesSeleccion;
}

export function MejoresRotaciones({ rotaciones, elegida = null, acciones }: Props) {
  const mejores = rotaciones.slice(0, 3);
  const [abierta, setAbierta] = useState<string | null>(
    mejores.some((r) => r.id === elegida) ? elegida : (mejores[0]?.id ?? null),
  );

  return (
    <section aria-labelledby="titulo-rotaciones" className="flex flex-col gap-3 2xl:col-start-1 2xl:row-start-1">
      <h2 id="titulo-rotaciones" className="text-[16px] font-semibold md:text-[18px]">
        Las {mejores.length} mejores rotaciones para su lote
      </h2>
      {acciones?.error && (
        <p role="alert" className="rounded-xl bg-roja-fondo p-3 text-[14px] font-semibold text-roja-texto">
          {acciones.error}
        </p>
      )}
      {mejores.length === 0 && (
        <p className="rounded-2xl border border-borde bg-white p-4 text-[15px] text-tinta-suave">
          Con el clima que se espera no encontramos una rotación que le convenga a este lote. Revise el ranking de
          cultivos.
        </p>
      )}
      <ol className="flex flex-col gap-3">
        {mejores.map((r, i) => (
          <TarjetaRotacion
            key={r.id}
            rotacion={r}
            posicion={i + 1}
            abierta={abierta === r.id}
            onAlternar={() => setAbierta((actual) => (actual === r.id ? null : r.id))}
            elegida={elegida === r.id}
            acciones={acciones}
          />
        ))}
      </ol>
    </section>
  );
}
