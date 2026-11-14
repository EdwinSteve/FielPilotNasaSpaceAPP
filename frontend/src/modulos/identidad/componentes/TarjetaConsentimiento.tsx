"use client";

// Autorización del tratamiento de datos (Ley 1581 de 2012), con su estado de carga y de error.
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
      <div className="flex items-center gap-3 rounded-xl border-2 border-borde-campo bg-white/70 p-4 sm:p-5">
        <Spinner className="h-5 w-5 text-campo" />
        <span className="text-sm font-semibold text-tinta/80 sm:text-base">Cargando la política de datos...</span>
      </div>
    );
  }

  if (errorCarga) {
    return (
      <div role="alert" className="flex items-center justify-between gap-3 rounded-xl border-2 border-error bg-error-fondo p-4 sm:p-5">
        <span className="text-sm font-semibold text-error">{errorCarga}</span>
        <button
          type="button"
          onClick={alReintentar}
          className="min-h-11 shrink-0 rounded-lg bg-error px-3 text-sm font-bold text-white hover:bg-error-oscuro"
        >
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <div className={`rounded-xl border-2 p-4 transition-colors sm:p-5 ${error ? "border-error bg-error-fondo" : "border-borde-campo bg-white"}`}>
      <div className="flex items-start gap-3.5">
        <input
          id={ID}
          type="checkbox"
          checked={aceptada}
          disabled={disabled}
          onChange={(e) => alCambiar(e.target.checked)}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${ID}-error` : undefined}
          className="mt-1 h-7 w-7 min-w-7 shrink-0 cursor-pointer rounded-lg accent-campo"
        />
        <div className="text-left text-sm leading-relaxed text-tinta sm:text-base">
          <label htmlFor={ID} className="cursor-pointer">
            Autorizo el tratamiento de mis datos personales según la <strong className="text-campo">Ley 1581 de 2012</strong>{" "}
            {version ? `(versión ${version}) ` : ""}
            solo para recibir recomendaciones agroclimáticas y alertas de mi finca.
          </label>{" "}
          <Link href="/politica-de-datos" target="_blank" className="font-bold text-campo underline hover:text-campo-oscuro">
            Ver política de datos
          </Link>
        </div>
      </div>
      {error && (
        <span id={`${ID}-error`} aria-live="polite" className="mt-2.5 block text-left text-sm font-semibold text-error">
          {error}
        </span>
      )}
    </div>
  );
}
