// Piezas comunes de las tarjetas de alertas: encabezado con fuente, escala de niveles,
// chips y el aviso de "sin datos" con su botón Reintentar.
import type { ReactNode } from "react";
import { Icono } from "@/compartido/ui/Icono";
import { FALLOS, TONOS, TONOS_ESCALA, type Tono } from "./niveles";
import type { FalloConsulta } from "../tipos";

export const CLASE_TARJETA = "flex flex-col rounded-2xl border border-borde bg-white p-4 2xl:p-6";

export function EncabezadoTarjeta({
  id,
  titulo,
  fuente,
  icono,
}: {
  id?: string;
  titulo: string;
  fuente?: string;
  icono?: { nombre: string; fondo: string };
}) {
  return (
    <div className="flex items-center justify-between gap-2">
      <div className="flex min-w-0 items-center gap-[10px]">
        {icono && (
          <span className={`rounded-[10px] p-2 ${icono.fondo}`}>
            <Icono nombre={icono.nombre} tam={18} />
          </span>
        )}
        <h2 id={id} className="text-[17px] font-semibold whitespace-nowrap">
          {titulo}
        </h2>
      </div>
      {fuente && (
        <span className="rounded-md bg-fondo px-2 py-[3px] text-[11px] font-semibold whitespace-nowrap text-tinta-suave">
          {fuente}
        </span>
      )}
    </div>
  );
}

/** Cuatro barras (normal → roja); la del nivel vigente va en color pleno y con su nombre en negrita. */
export function EscalaNiveles({ nombres, vigente, titulo }: { nombres: string[]; vigente: number | null; titulo: string }) {
  return (
    <ol aria-label={titulo} className="flex gap-[6px]">
      {nombres.map((nombre, i) => {
        const tono = TONOS[TONOS_ESCALA[i]];
        const activo = i === vigente;
        return (
          <li
            key={nombre}
            aria-current={activo ? "true" : undefined}
            className="flex min-w-0 flex-1 flex-col gap-[6px]"
          >
            <span className={`h-2 rounded-full ${activo ? tono.barra : tono.barraTenue}`} />
            <span
              className={`text-[11px] 2xl:text-[12px] ${activo ? `font-bold ${tono.texto}` : "font-medium text-tinta-suave"}`}
            >
              {nombre}
            </span>
          </li>
        );
      })}
    </ol>
  );
}

/** Chip de nivel ("ALERTA NARANJA", "Ríos: Amarilla"). El tono verde va sobre blanco, como en el diseño. */
export function Chip({ tono, children, sobreBlanco = false }: { tono: Tono; children: ReactNode; sobreBlanco?: boolean }) {
  const t = TONOS[tono];
  const fondo = sobreBlanco || tono === "gris" ? "bg-white" : t.fondo;
  return (
    <span className={`inline-flex rounded-full px-[10px] py-[5px] text-[11px] font-bold whitespace-nowrap ${fondo} ${t.texto}`}>
      {children}
    </span>
  );
}

/** Aviso de "sin datos" (400, 502, 503) para ríos o sequía. */
export function AvisoFallo({
  fuente,
  fallo,
  alReintentar,
}: {
  fuente: "rios" | "sequia";
  fallo: FalloConsulta;
  alReintentar: () => void;
}) {
  const aviso = FALLOS[fuente][fallo];
  const tono = TONOS[aviso.tono];
  return (
    <div role="alert" className={`flex flex-wrap items-center gap-[14px] rounded-xl p-4 ${tono.fondo}`}>
      <span className="rounded-[10px] bg-white p-[10px]">
        <Icono nombre={aviso.icono} tam={22} />
      </span>
      <div className="flex min-w-[180px] flex-1 flex-col gap-[3px]">
        <p className={`text-[12px] font-bold ${tono.texto}`}>{aviso.etiqueta}</p>
        <p className="text-[15px] leading-[1.45] font-semibold">{aviso.mensaje}</p>
      </div>
      {aviso.reintentable && (
        <button
          type="button"
          onClick={alReintentar}
          className="rounded-[10px] bg-verde-700 px-4 py-[10px] text-[14px] font-semibold text-white transition-colors hover:bg-verde-900 max-md:w-full"
        >
          Reintentar
        </button>
      )}
    </div>
  );
}

/** Mientras llega la respuesta. */
export function AvisoCargando({ texto }: { texto: string }) {
  return (
    <div role="status" className="flex animate-pulse items-center gap-[14px] rounded-xl bg-fondo p-4">
      <span className="size-[42px] rounded-[10px] bg-white" />
      <p className="text-[15px] font-semibold text-tinta-suave">{texto}</p>
    </div>
  );
}
