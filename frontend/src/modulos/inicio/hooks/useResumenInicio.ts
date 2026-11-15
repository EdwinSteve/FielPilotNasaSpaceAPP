"use client";

// Trae los datos de la pantalla de inicio. Se llama ya con sesión: el cliente HTTP agrega el token.
import { useCallback, useEffect, useState } from "react";
import { aErrorApi, type ErrorApi } from "@/compartido/api/tipos";
import { obtenerResumenInicio } from "../api";
import type { DatosInicio } from "../tipos";

export function useResumenInicio() {
  const [datos, setDatos] = useState<DatosInicio | null>(null);
  const [error, setError] = useState<ErrorApi | null>(null);
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let cancelado = false;
    setError(null);
    obtenerResumenInicio()
      .then((d) => !cancelado && setDatos(d))
      .catch((err) => !cancelado && setError(aErrorApi(err)));
    return () => {
      cancelado = true;
    };
  }, [intento]);

  const reintentar = useCallback(() => setIntento((n) => n + 1), []);
  return { datos, error, reintentar };
}
