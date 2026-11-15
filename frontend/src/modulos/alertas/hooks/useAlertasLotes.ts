"use client";

// Consulta ríos (IDEAM) y sequía (NASA POWER) de todos los lotes en paralelo.
// `actualizar` vuelve a consultar todos; `reintentar` solo un lote.
import { useCallback, useEffect, useState } from "react";
import { consultarRios, consultarSequia } from "../api";
import type { AlertasLote, LoteUbicado, Simulacion } from "../tipos";

const CARGANDO: AlertasLote = { rios: { estado: "cargando" }, sequia: { estado: "cargando" } };

export function useAlertasLotes(lotes: LoteUbicado[], simular?: Simulacion) {
  const [alertas, setAlertas] = useState<Record<string, AlertasLote>>({});
  const [pendientes, setPendientes] = useState(0);

  const consultarLote = useCallback(
    async (lote: LoteUbicado) => {
      setAlertas((a) => ({ ...a, [lote.id]: CARGANDO }));
      setPendientes((n) => n + 1);
      const [rios, sequia] = await Promise.all([consultarRios(lote, simular), consultarSequia(lote, simular)]);
      setAlertas((a) => ({ ...a, [lote.id]: { rios, sequia } }));
      setPendientes((n) => n - 1);
    },
    [simular],
  );

  const actualizar = useCallback(() => lotes.forEach(consultarLote), [lotes, consultarLote]);

  // Los lotes llegan como prop nueva en cada navegación (?lote=...): se vuelve a consultar
  // solo si cambia la lista de lotes, no al cambiar el lote elegido.
  const claveLotes = lotes.map((l) => `${l.id}:${l.lat}:${l.lon}`).join("|");
  useEffect(() => {
    actualizar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [claveLotes, simular]);

  return {
    alertasDe: (id: string): AlertasLote => alertas[id] ?? CARGANDO,
    actualizando: pendientes > 0,
    actualizar,
    reintentar: (lote: LoteUbicado) => void consultarLote(lote),
  };
}
