"use client";

// Los terrenos del productor (GET /fincas/terrenos) con su ubicación. Se piden en el navegador
// porque exigen el token de la sesión.
import { useCallback, useEffect, useState } from "react";
import { aErrorApi, type ErrorApi } from "@/compartido/api/tipos";
import { obtenerLotesUbicados } from "../api";
import type { LoteUbicado } from "../tipos";

export type EstadoLotes =
  | { fase: "cargando" }
  | { fase: "listo"; lotes: LoteUbicado[] }
  | { fase: "error"; error: ErrorApi };

export function useLotesUbicados() {
  const [estado, setEstado] = useState<EstadoLotes>({ fase: "cargando" });

  const cargar = useCallback(async () => {
    setEstado({ fase: "cargando" });
    try {
      setEstado({ fase: "listo", lotes: await obtenerLotesUbicados() });
    } catch (e) {
      setEstado({ fase: "error", error: aErrorApi(e) });
    }
  }, []);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  return { estado, reintentar: cargar };
}
