// Menú para cambiar de lote: "El Mirador · 4,2 ha". Al elegir otro, abre `destino(id)`
// (por defecto, el plan de siembra de ese lote).
"use client";

import { useRouter } from "next/navigation";
import { useEffect, useId, useRef, useState } from "react";
import { Icono } from "@/compartido/ui/Icono";
import type { LoteOpcion } from "../tipos";

const etiqueta = (l: LoteOpcion) => `${l.nombre} · ${l.area}`;

const planDeSiembra = (id: string) => `/lotes/${id}/rotacion`;

interface Props {
  lotes: LoteOpcion[];
  actual: LoteOpcion;
  /** Ruta que se abre al elegir un lote. */
  destino?: (loteId: string) => string;
}

export function SelectorLote({ lotes, actual, destino = planDeSiembra }: Props) {
  const router = useRouter();
  const idMenu = useId();
  const [abierto, setAbierto] = useState(false);
  const contenedor = useRef<HTMLDivElement>(null);
  const boton = useRef<HTMLButtonElement>(null);
  const opciones = useRef<(HTMLLIElement | null)[]>([]);

  // Al abrir, el foco va al lote actual; un clic fuera lo cierra.
  useEffect(() => {
    if (!abierto) return;
    opciones.current[lotes.findIndex((l) => l.id === actual.id)]?.focus();
    const fuera = (e: PointerEvent) => {
      if (!contenedor.current?.contains(e.target as Node)) setAbierto(false);
    };
    document.addEventListener("pointerdown", fuera);
    return () => document.removeEventListener("pointerdown", fuera);
  }, [abierto, lotes, actual.id]);

  const cerrar = () => {
    setAbierto(false);
    boton.current?.focus();
  };

  const elegir = (lote: LoteOpcion) => {
    cerrar();
    if (lote.id !== actual.id) router.push(destino(lote.id));
  };

  const teclado = (e: React.KeyboardEvent<HTMLLIElement>, i: number) => {
    const mover = (destino: number) => opciones.current[(destino + lotes.length) % lotes.length]?.focus();
    if (e.key === "ArrowDown") mover(i + 1);
    else if (e.key === "ArrowUp") mover(i - 1);
    else if (e.key === "Home") mover(0);
    else if (e.key === "End") mover(lotes.length - 1);
    else if (e.key === "Enter" || e.key === " ") elegir(lotes[i]);
    else if (e.key === "Escape" || e.key === "Tab") cerrar();
    else return;
    e.preventDefault();
  };

  return (
    <div ref={contenedor} className="relative w-full md:w-auto">
      <button
        ref={boton}
        type="button"
        aria-haspopup="listbox"
        aria-expanded={abierto}
        aria-controls={idMenu}
        onClick={() => setAbierto((a) => !a)}
        className={`flex w-full items-center justify-between gap-[6px] rounded-xl border bg-white px-[14px] py-[11px] md:justify-start md:px-3 md:py-[9px] 2xl:gap-2 2xl:px-4 2xl:py-[10px] ${
          abierto ? "border-verde-700 outline outline-1 outline-verde-700" : "border-borde"
        }`}
      >
        <span className="flex items-center gap-2 text-[14px] font-semibold md:gap-[6px] md:text-[13px] 2xl:gap-2 2xl:text-[14px]">
          <Icono nombre="pin-oscuro" tam={18} className="size-4 2xl:size-[18px]" />
          <span className="sr-only">Lote: </span>
          {etiqueta(actual)}
        </span>
        <span aria-hidden className="text-[12px] text-tinta-suave md:text-[11px] 2xl:text-[12px]">
          {abierto ? "▴" : "▾"}
        </span>
      </button>

      {abierto && (
        <ul
          id={idMenu}
          role="listbox"
          aria-label="Sus lotes"
          className="absolute inset-x-0 top-full z-20 mt-2 flex flex-col gap-[2px] rounded-xl border border-borde bg-white p-[6px] shadow-[0_8px_24px_rgba(18,36,26,0.12)] md:left-auto md:w-[260px]"
        >
          {lotes.map((l, i) => {
            const seleccionado = l.id === actual.id;
            return (
              <li
                key={l.id}
                ref={(el) => {
                  opciones.current[i] = el;
                }}
                role="option"
                aria-selected={seleccionado}
                tabIndex={-1}
                onClick={() => elegir(l)}
                onKeyDown={(e) => teclado(e, i)}
                className={`flex cursor-pointer items-center justify-between rounded-lg p-[10px] text-[14px] outline-none focus-visible:ring-2 focus-visible:ring-verde-500 ${
                  seleccionado ? "bg-verde-100 font-semibold text-verde-700" : "font-medium hover:bg-fondo"
                }`}
              >
                {etiqueta(l)}
                {seleccionado && <Icono nombre="check" tam={16} />}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
