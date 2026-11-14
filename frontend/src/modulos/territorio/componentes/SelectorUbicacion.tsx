"use client";

// Departamento y municipio de Colombia, uno dependiente del otro.
// Al cambiar de departamento se borra el municipio, que solo se habilita con un departamento elegido.
import { CampoSelector } from "@/compartido/ui";
import { useDepartamentos, useMunicipios } from "../hooks/useMunicipios";
import type { Ubicacion } from "../tipos";

interface Props {
  /** Prefijo de los id de los campos: `${id}-departamento` y `${id}-municipio`. */
  id: string;
  valor: Ubicacion;
  alCambiar: (valor: Ubicacion) => void;
  etiquetaDepartamento?: string;
  etiquetaMunicipio?: string;
  errores?: { departamento?: string; municipio?: string };
  disabled?: boolean;
  /** Clases extra del contenedor, por ejemplo para ponerlo en dos columnas dentro de una grilla. */
  className?: string;
}

export function SelectorUbicacion({
  id,
  valor,
  alCambiar,
  etiquetaDepartamento = "Departamento",
  etiquetaMunicipio = "Municipio",
  errores = {},
  disabled = false,
  className = "",
}: Props) {
  const departamentos = useDepartamentos();
  const municipios = useMunicipios(valor.departamento);
  const sinDepartamento = !valor.departamento;

  return (
    <fieldset className={`m-0 flex min-w-0 flex-col gap-5 border-none p-0 ${className}`} disabled={disabled}>
      <legend className="sr-only">Ubicación</legend>
      <CampoSelector
        id={`${id}-departamento`}
        etiqueta={etiquetaDepartamento}
        obligatorio
        valor={valor.departamento}
        alCambiar={(departamento) => alCambiar({ departamento, municipio: "" })}
        opciones={departamentos.lista}
        marcador="Elija su departamento"
        cargando={departamentos.cargando}
        error={errores.departamento ?? departamentos.error ?? undefined}
        alReintentar={departamentos.error ? departamentos.reintentar : undefined}
        disabled={disabled}
      />
      <CampoSelector
        id={`${id}-municipio`}
        etiqueta={etiquetaMunicipio}
        obligatorio
        valor={valor.municipio}
        alCambiar={(municipio) => alCambiar({ ...valor, municipio })}
        opciones={municipios.lista}
        marcador={sinDepartamento ? "Primero elija el departamento" : "Elija su municipio"}
        cargando={municipios.cargando}
        error={errores.municipio ?? municipios.error ?? undefined}
        alReintentar={municipios.error ? municipios.reintentar : undefined}
        disabled={disabled || sinDepartamento}
      />
    </fieldset>
  );
}
