"use client";

// Autorización del tratamiento de datos (Ley 1581 de 2012), con su estado de carga y de error.
// Mismos estándares que los campos: borde de 1 px que se ve (borde-campo), radio de 10 px, rojo del diseño.
import Link from "next/link";
import { Spinner } from "@/compartido/ui";

interface Props {
  aceptada: boolean;
  alCambiar: (aceptada: boolean) => void;
  version?: string;
  cargando: boolean;
  errorCarga: string | null;
  alReintentar: () => void;
  error?: string;
  disabled?: boolean;
}

const ID = "consentimiento-datos";

export function TarjetaConsentimiento({ aceptada, alCambiar, version, cargando, errorCarga, alReintentar, error, disabled }: Props) {
  if (cargando) {
    return (
      <div className="flex items-center gap-3 rounded-[10px] border border-borde-campo bg-white/70 p-4">
        <Spinner className="h-5 w-5 text-verde-700" />
        <span className="text-[14px] font-medium text-tinta-suave">Cargando la política de datos...</span>
      </div>
    );
  }

  if (errorCarga) {
    return (
      <div role="alert" className="flex items-center justify-between gap-3 rounded-[10px] border border-roja-texto/25 bg-roja-fondo p-4">
        <span className="text-[14px] font-medium text-roja-texto">{errorCarga}</span>
        <button
          type="button"
          onClick={alReintentar}
          className="min-h-11 shrink-0 rounded-[10px] bg-roja-texto px-3 text-[14px] font-semibold text-white hover:opacity-90"
        >
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <div
      className={`rounded-[10px] border p-4 transition-colors ${error ? "border-roja-texto bg-roja-fondo" : "border-borde-campo bg-white"}`}
    >
      <div className="flex items-start gap-3">
        <input
          id={ID}
          type="checkbox"
          checked={aceptada}
          disabled={disabled}
          onChange={(e) => alCambiar(e.target.checked)}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${ID}-error` : undefined}
          className="mt-0.5 size-6 min-w-6 shrink-0 cursor-pointer accent-verde-700"
        />
        <div className="text-left text-[14px] leading-relaxed text-tinta">
          <label htmlFor={ID} className="cursor-pointer">
            Autorizo el tratamiento de mis datos personales según la <strong className="font-semibold">Ley 1581 de 2012</strong>{" "}
            {version ? `(versión ${version}) ` : ""}
            solo para recibir recomendaciones agroclimáticas y alertas de mi finca.
          </label>{" "}
          <Link href="/politica-de-datos" target="_blank" className="font-semibold text-verde-700 underline hover:text-verde-900">
            Ver política de datos
          </Link>
        </div>
      </div>
      {error && (
        <span id={`${ID}-error`} aria-live="polite" className="mt-2.5 block text-left text-[13px] font-medium text-roja-texto">
          {error}
        </span>
      )}
    </div>
  );
}
