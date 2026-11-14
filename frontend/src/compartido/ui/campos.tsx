"use client";

// Campos de formulario con los estándares del diseño de Figma: bordes de 1 px, esquinas de 10 px,
// etiquetas en tinta seminegrita y foco verde. Dos decisiones de accesibilidad:
// - 48 px de alto: blanco cómodo para el pulgar.
// - Texto de 16 px: con menos, el iPhone hace zoom al tocar el campo.
import { useState, type InputHTMLAttributes, type ReactNode } from "react";
import { IconoAlerta, Spinner } from "./iconos";

export function claseEntrada(error: string | undefined, bloqueado = false): string {
  return [
    "h-12 w-full rounded-[10px] border px-[14px] text-[16px] font-medium text-tinta outline-none transition-colors",
    "placeholder:font-normal placeholder:text-tinta-suave",
    bloqueado ? "cursor-not-allowed bg-fondo text-tinta-suave" : "bg-white",
    error
      ? "border-roja-texto focus:ring-2 focus:ring-roja-texto/20"
      : "border-borde-campo focus:border-verde-700 focus:ring-2 focus:ring-verde-500/25",
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
    <label htmlFor={htmlFor} className="mb-1.5 flex items-center justify-between text-[14px] font-semibold text-tinta">
      <span>
        {texto} {obligatorio && <span className="text-roja-texto">*</span>}
        {opcional && <span className="text-[13px] font-normal text-tinta-suave">(opcional)</span>}
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
      <div id={`${id}-error`} aria-live="polite" className="mt-1.5 flex items-start gap-1.5 text-[13px] font-medium text-roja-texto">
        <IconoAlerta className="mt-px h-4 w-4" />
        <span>
          {error}
          {accion}
        </span>
      </div>
    );
  }
  return ayuda ? (
    <span id={`${id}-ayuda`} className="mt-1.5 text-[13px] text-tinta-suave">
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
        <span className="pointer-events-none absolute left-[14px] border-r border-borde-campo pr-3 text-[16px] font-semibold text-tinta">
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
          className={`${claseEntrada(error, disabled)} pl-[68px]`}
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
          className={`${claseEntrada(error, disabled)} pr-[76px]`}
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-pressed={visible}
          aria-label={visible ? "Ocultar contraseña" : "Mostrar contraseña"}
          className="absolute top-1/2 right-1 min-h-10 -translate-y-1/2 rounded-[8px] px-3 text-[14px] font-semibold text-verde-700 hover:bg-verde-100"
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
            <span className="flex items-center gap-1.5 text-[12px] font-medium text-verde-700">
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
          className="pointer-events-none absolute top-1/2 right-[14px] h-5 w-5 -translate-y-1/2 text-tinta-suave"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
        </svg>
      </div>
      <AyudaCampo
        id={id}
        error={error}
        ayuda={ayuda}
        accion={
          alReintentar && (
            <button type="button" onClick={alReintentar} className="ml-1 font-semibold text-verde-700 underline">
              Reintentar
            </button>
          )
        }
      />
    </div>
  );
}
