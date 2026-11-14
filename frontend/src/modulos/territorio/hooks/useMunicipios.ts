"use client";

import { useCallback, useEffect, useState } from "react";
import { listarDepartamentos, listarMunicipios } from "../api";
import type { Departamento, Municipio } from "../tipos";

interface Estado<T> {
  lista: T[];
  cargando: boolean;
  error: string | null;
  reintentar: () => void;
}

/** Carga una lista con cargando, error y reintentar. Con `cargar` nulo, la lista queda vacía. */
function useLista<T>(cargar: (() => Promise<T[]>) | null, mensajeError: string): Estado<T> {
  const [lista, setLista] = useState<T[]>([]);
  const [cargando, setCargando] = useState(Boolean(cargar));
  const [error, setError] = useState<string | null>(null);
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    setLista([]);
    setError(null);
    if (!cargar) {
      setCargando(false);
      return;
    }
    let cancelado = false;
    setCargando(true);
    cargar()
      .then((l) => !cancelado && setLista(l))
      .catch(() => !cancelado && setError(mensajeError))
      .finally(() => !cancelado && setCargando(false));
    return () => {
      cancelado = true;
    };
  }, [cargar, mensajeError, intento]);

  const reintentar = useCallback(() => setIntento((n) => n + 1), []);
  return { lista, cargando, error, reintentar };
}

export function useDepartamentos(): Estado<Departamento> {
  return useLista(listarDepartamentos, "No pudimos cargar los departamentos.");
}

/** Municipios del departamento elegido. Sin departamento, la lista queda vacía y no se consulta nada. */
export function useMunicipios(departamento: string): Estado<Municipio> {
  const cargar = useCallback(() => listarMunicipios(departamento), [departamento]);
  return useLista(departamento ? cargar : null, "No pudimos cargar los municipios.");
}
