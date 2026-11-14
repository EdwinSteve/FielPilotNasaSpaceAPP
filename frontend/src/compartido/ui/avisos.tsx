// Avisos y botones con los estándares del diseño de Figma: tarjetas de 16 px de radio con borde de 1 px,
// botones de 10 px de radio en verde-700, y los colores de alerta del diseño (roja, verde).
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { IconoAlerta, IconoVerificado, Spinner } from "./iconos";

/** Botón principal (verde). Exportado como clase para usarlo también en enlaces (<Link>). */
export const CLASE_BOTON_PRINCIPAL =
  "flex min-h-12 w-full items-center justify-center gap-2 rounded-[10px] bg-verde-700 px-5 text-[15px] font-semibold text-white transition-colors hover:bg-verde-900 disabled:opacity-60";

/** Botón secundario (blanco con borde), como el selector de lote del diseño. */
export const CLASE_BOTON_SECUNDARIO =
  "flex min-h-12 w-full items-center justify-center gap-2 rounded-[10px] border border-borde bg-white px-5 text-[15px] font-semibold text-tinta transition-colors hover:border-verde-500 hover:text-verde-700";

interface PropsTarjetaError {
  mensaje: string;
  codigo?: string;
  alReintentar?: () => void;
  textoReintentar?: string;
}

export function TarjetaError({ mensaje, codigo, alReintentar, textoReintentar = "Reintentar" }: PropsTarjetaError) {
  return (
    <div role="alert" className="flex flex-col gap-3 rounded-2xl border border-roja-texto/25 bg-roja-fondo p-4 text-left">
      <div className="flex items-start gap-3">
        <IconoAlerta className="mt-0.5 h-5 w-5 text-roja-texto" />
        <div>
          <p className="text-[15px] font-semibold text-tinta">Algo salió mal</p>
          <p className="mt-0.5 text-[14px] text-tinta-suave">{mensaje}</p>
          {codigo && <p className="mt-1 font-mono text-[12px] text-tinta-suave">Código: {codigo}</p>}
        </div>
      </div>
      {alReintentar && (
        <button
          type="button"
          onClick={alReintentar}
          className="min-h-11 w-full rounded-[10px] bg-roja-texto text-[14px] font-semibold text-white hover:opacity-90"
        >
          {textoReintentar}
        </button>
      )}
    </div>
  );
}

export function AvisoExito({ titulo, detalle }: { titulo: string; detalle: string }) {
  return (
    <div role="status" className="flex items-center gap-3 rounded-2xl border border-verde-500/30 bg-verde-100 p-4 text-left">
      <IconoVerificado className="h-6 w-6 text-verde-700" />
      <div>
        <p className="text-[16px] font-semibold text-tinta">{titulo}</p>
        <p className="text-[14px] text-tinta-suave">{detalle}</p>
      </div>
    </div>
  );
}

type PropsBoton = ButtonHTMLAttributes<HTMLButtonElement> & {
  cargando?: boolean;
  textoCargando?: string;
  children: ReactNode;
};

export function BotonPrincipal({ cargando = false, textoCargando, children, className = "", disabled, ...resto }: PropsBoton) {
  return (
    <button disabled={disabled || cargando} aria-busy={cargando} className={`${CLASE_BOTON_PRINCIPAL} ${className}`} {...resto}>
      {cargando && <Spinner />}
      {cargando && textoCargando ? textoCargando : children}
    </button>
  );
}
