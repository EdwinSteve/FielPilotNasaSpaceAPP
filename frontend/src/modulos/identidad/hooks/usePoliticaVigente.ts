"use client";

import { useCallback, useEffect, useState } from "react";
import { obtenerPoliticaVigente } from "../api";
import type { PoliticaVigente } from "../tipos";

/** Carga la política de datos vigente. `recargar` se usa cuando el backend dice que cambió. */
export function usePoliticaVigente() {
  const [politica, setPolitica] = useState<PoliticaVigente | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let cancelado = false;
    setCargando(true);
    setError(null);
    obtenerPoliticaVigente()
      .then((p) => !cancelado && setPolitica(p))
      .catch(() => !cancelado && setError("No pudimos cargar la política de datos."))
      .finally(() => !cancelado && setCargando(false));
    return () => {
      cancelado = true;
    };
  }, [intento]);

  const recargar = useCallback(() => setIntento((n) => n + 1), []);
  return { politica, cargando, error, recargar };
}
