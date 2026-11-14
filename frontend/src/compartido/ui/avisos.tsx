// Avisos y botones comunes: tarjeta de error, aviso de éxito y botón principal con estado de carga.
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { IconoAlerta, IconoVerificado, Spinner } from "./iconos";

interface PropsTarjetaError {
  mensaje: string;
  codigo?: string;
  alReintentar?: () => void;
  textoReintentar?: string;
}

export function TarjetaError({ mensaje, codigo, alReintentar, textoReintentar = "Reintentar" }: PropsTarjetaError) {
  return (
    <div role="alert" className="flex flex-col gap-3 rounded-2xl border-2 border-error bg-error-fondo p-4 text-left">
      <div className="flex items-start gap-3">
        <IconoAlerta className="mt-0.5 h-6 w-6 text-error" />
        <div>
          <p className="text-base font-extrabold text-tinta">Algo salió mal</p>
          <p className="text-sm text-tinta/80">{mensaje}</p>
          {codigo && <p className="mt-1 font-mono text-sm text-tinta/60">Código: {codigo}</p>}
        </div>
      </div>
      {alReintentar && (
        <button
          type="button"
          onClick={alReintentar}
          className="min-h-14 w-full rounded-xl bg-error text-base font-bold text-white hover:bg-error-oscuro"
        >
          {textoReintentar}
        </button>
      )}
    </div>
  );
}

export function AvisoExito({ titulo, detalle }: { titulo: string; detalle: string }) {
  return (
    <div role="status" className="flex items-center gap-3 rounded-2xl border-2 border-campo bg-arena p-4 text-left">
      <IconoVerificado className="h-6 w-6 text-campo" />
      <div>
        <p className="text-lg font-extrabold text-tinta">{titulo}</p>
        <p className="text-tinta/80">{detalle}</p>
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
    <button
      disabled={disabled || cargando}
      aria-busy={cargando}
      className={`flex min-h-14 w-full items-center justify-center gap-2 rounded-2xl bg-campo px-6 font-titulo text-lg font-extrabold text-white shadow-md hover:bg-campo-oscuro disabled:opacity-60 ${className}`}
      {...resto}
    >
      {cargando && <Spinner />}
      {cargando && textoCargando ? textoCargando : children}
    </button>
  );
}
