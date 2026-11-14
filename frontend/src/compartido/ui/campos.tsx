"use client";

// Campos de formulario de SURCO: grandes (56 px), con etiqueta, ayuda y error accesibles.
import { useState, type InputHTMLAttributes, type ReactNode } from "react";
import { IconoAlerta, Spinner } from "./iconos";

export function claseEntrada(error: string | undefined, bloqueado = false): string {
  return [
    "w-full min-h-14 h-14 px-4 rounded-xl text-lg font-semibold text-tinta border-2 outline-none transition-all",
    bloqueado ? "bg-arena-oscura cursor-not-allowed opacity-80" : "bg-white",
    error
      ? "border-error focus:ring-2 focus:ring-error/20"
      : "border-borde-campo focus:border-campo focus:ring-2 focus:ring-campo/20",
  ].join(" ");
}

interface PropsEtiqueta {
  htmlFor: string;
  texto: string;
  obligatorio?: boolean;
  opcional?: boolean;
  extra?: ReactNode;
}

export function Etiqueta({ htmlFor, texto, obligatorio, opcional, extra }: PropsEtiqueta) {
  return (
    <label htmlFor={htmlFor} className="mb-1.5 flex items-center justify-between text-base font-bold text-tinta sm:text-lg">
      <span>
        {texto} {obligatorio && <span className="text-error">*</span>}
        {opcional && <span className="text-sm font-semibold text-tinta/60">(opcional)</span>}
      </span>
      {extra}
    </label>
  );
}

interface PropsAyuda {
  id: string;
  error?: string;
  ayuda?: string;
  accion?: ReactNode;
}

/** Muestra el error del campo o, si no hay error, su texto de ayuda. */
export function AyudaCampo({ id, error, ayuda, accion }: PropsAyuda) {
  if (error) {
    return (
      <div id={`${id}-error`} aria-live="polite" className="mt-1.5 flex items-start gap-1.5 text-sm font-semibold text-error">
        <IconoAlerta className="mt-0.5 h-4 w-4" />
        <span>
          {error}
          {accion}
        </span>
      </div>
    );
  }
  return ayuda ? (
    <span id={`${id}-ayuda`} className="mt-1.5 text-sm text-tinta/70">
      {ayuda}
    </span>
  ) : null;
}

function describidoPor(id: string, error?: string, ayuda?: string): string | undefined {
  if (error) return `${id}-error`;
  return ayuda ? `${id}-ayuda` : undefined;
}

type PropsEntradaBase = Omit<InputHTMLAttributes<HTMLInputElement>, "onChange" | "value" | "id"> & {
  id: string;
  etiqueta: string;
  valor: string;
  alCambiar: (valor: string) => void;
  error?: string;
  ayuda?: string;
  obligatorio?: boolean;
  opcional?: boolean;
};

export function CampoTexto({ id, etiqueta, valor, alCambiar, error, ayuda, obligatorio, opcional, disabled, ...resto }: PropsEntradaBase) {
  return (
    <div className="flex w-full flex-col text-left">
      <Etiqueta htmlFor={id} texto={etiqueta} obligatorio={obligatorio} opcional={opcional} />
      <input
        id={id}
        value={valor}
        onChange={(e) => alCambiar(e.target.value)}
        disabled={disabled}
        aria-invalid={Boolean(error)}
        aria-describedby={describidoPor(id, error, ayuda)}
        className={claseEntrada(error, disabled)}
        {...resto}
      />
      <AyudaCampo id={id} error={error} ayuda={ayuda} />
    </div>
  );
}

interface PropsCelular {
  id: string;
  valor: string;
  alCambiar: (valor: string) => void;
  error?: string;
  ayuda?: string;
  disabled?: boolean;
  /** Acción junto al error, por ejemplo "Ingresar aquí" cuando el celular ya existe. */
  accionError?: ReactNode;
}

/** Celular colombiano: prefijo +57 fijo y solo 10 dígitos. */
export function CampoCelular({ id, valor, alCambiar, error, ayuda, disabled, accionError }: PropsCelular) {
  return (
    <div className="flex w-full flex-col text-left">
      <Etiqueta htmlFor={id} texto="Número de celular" obligatorio />
      <div className="relative flex items-center">
        <span className="pointer-events-none absolute left-3 border-r-2 border-borde-campo pr-3 text-base font-extrabold text-tinta sm:text-lg">
          +57
        </span>
        <input
          id={id}
          type="tel"
          inputMode="numeric"
          autoComplete="tel-national"
          value={valor}
          onChange={(e) => alCambiar(e.target.value.replace(/\D/g, "").slice(0, 10))}
          disabled={disabled}
          placeholder="314 000 0000"
          aria-invalid={Boolean(error)}
          aria-describedby={describidoPor(id, error, ayuda)}
          className={`${claseEntrada(error, disabled)} pl-[72px]`}
        />
      </div>
      <AyudaCampo id={id} error={error} ayuda={ayuda} accion={accionError} />
    </div>
  );
}

interface PropsContrasena {
  id: string;
  valor: string;
  alCambiar: (valor: string) => void;
  error?: string;
  ayuda?: string;
  disabled?: boolean;
  autoComplete: "current-password" | "new-password";
  placeholder?: string;
}

export function CampoContrasena({ id, valor, alCambiar, error, ayuda, disabled, autoComplete, placeholder }: PropsContrasena) {
  const [visible, setVisible] = useState(false);
  return (
    <div className="flex w-full flex-col text-left">
      <Etiqueta htmlFor={id} texto="Contraseña" obligatorio />
      <div className="relative">
        <input
          id={id}
          type={visible ? "text" : "password"}
          autoComplete={autoComplete}
          value={valor}
          onChange={(e) => alCambiar(e.target.value)}
          disabled={disabled}
          placeholder={placeholder}
          aria-invalid={Boolean(error)}
          aria-describedby={describidoPor(id, error, ayuda)}
          className={`${claseEntrada(error, disabled)} pr-[84px]`}
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-pressed={visible}
          aria-label={visible ? "Ocultar contraseña" : "Mostrar contraseña"}
          className="absolute top-1/2 right-2 min-h-11 -translate-y-1/2 px-3 font-bold text-campo underline"
        >
          {visible ? "Ocultar" : "Ver"}
        </button>
      </div>
      <AyudaCampo id={id} error={error} ayuda={ayuda} />
    </div>
  );
}

export interface OpcionSelector {
  codigo: string;
  nombre: string;
}

interface PropsSelector {
  id: string;
  etiqueta: string;
  valor: string;
  alCambiar: (valor: string) => void;
  opciones: OpcionSelector[];
  marcador?: string;
  cargando?: boolean;
  error?: string;
  ayuda?: string;
  obligatorio?: boolean;
  disabled?: boolean;
  alReintentar?: () => void;
}

export function CampoSelector({
  id,
  etiqueta,
  valor,
  alCambiar,
  opciones,
  marcador = "Seleccione una opción",
  cargando = false,
  error,
  ayuda,
  obligatorio,
  disabled,
  alReintentar,
}: PropsSelector) {
  const bloqueado = disabled || cargando;
  return (
    <div className="flex w-full flex-col text-left">
      <Etiqueta
        htmlFor={id}
        texto={etiqueta}
        obligatorio={obligatorio}
        extra={
          cargando && (
            <span className="flex items-center gap-1.5 text-xs font-semibold text-campo">
              <Spinner className="h-3.5 w-3.5" /> Cargando...
            </span>
          )
        }
      />
      <div className="relative">
        <select
          id={id}
          value={valor}
          onChange={(e) => alCambiar(e.target.value)}
          disabled={bloqueado}
          aria-invalid={Boolean(error)}
          aria-describedby={describidoPor(id, error, ayuda)}
          className={`${claseEntrada(error, bloqueado)} appearance-none pr-11 ${bloqueado ? "" : "cursor-pointer"}`}
        >
          <option value="" disabled>
            {cargando ? "Cargando opciones..." : marcador}
          </option>
          {opciones.map((o) => (
            <option key={o.codigo} value={o.codigo}>
              {o.nombre}
            </option>
          ))}
        </select>
        <svg
          className="pointer-events-none absolute top-1/2 right-4 h-5 w-5 -translate-y-1/2 text-tinta/60"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M19 9l-7 7-7-7" />
        </svg>
      </div>
      <AyudaCampo
        id={id}
        error={error}
        ayuda={ayuda}
        accion={
          alReintentar && (
            <button type="button" onClick={alReintentar} className="ml-1 font-black text-campo underline">
              Reintentar
            </button>
          )
        }
      />
    </div>
  );
}
