"use client";

// El plan de rotación de un lote con datos reales: lotes del productor, plan guardado o calculado con el
// modelo (puede tardar minutos la primera vez) y la elección del ciclo, optimista con vuelta atrás.
import { useCallback, useEffect, useRef, useState } from "react";
import type { ErrorApi } from "@/compartido/api/tipos";
import {
  aLoteOpcion,
  aPlanRotacion,
  calcularPlan,
  elegirRotacion,
  listarLotes,
  nombreMunicipio,
  obtenerPlanGuardado,
} from "../api";
import type { PlanRotacion } from "../tipos";

export type EstadoPlan =
  | { fase: "cargando" }
  | { fase: "calculando" }
  | { fase: "sin-lotes" }
  | { fase: "no-encontrado" }
  | { fase: "error"; error: ErrorApi }
  | { fase: "listo"; plan: PlanRotacion };

const errorInesperado: ErrorApi = {
  codigo: "ERROR_INTERNO",
  mensaje: "Ocurrió un error inesperado. Inténtelo de nuevo.",
};

const comoErrorApi = (e: unknown): ErrorApi =>
  e && typeof e === "object" && "mensaje" in e ? (e as ErrorApi) : errorInesperado;

export function usePlanRotacion(loteId: string) {
  const [estado, setEstado] = useState<EstadoPlan>({ fase: "cargando" });
  const [intento, setIntento] = useState(0);
  const [eligiendo, setEligiendo] = useState<string | null>(null);
  const [errorSeleccion, setErrorSeleccion] = useState<string | null>(null);
  const vigente = useRef(0);

  useEffect(() => {
    const consulta = ++vigente.current;
    const sigue = () => consulta === vigente.current;
    setEstado({ fase: "cargando" });
    (async () => {
      try {
        const { lotes, terrenos } = await listarLotes();
        if (!sigue()) return;
        if (terrenos.length === 0) return setEstado({ fase: "sin-lotes" });
        const terreno = terrenos.find((t) => t.id === loteId);
        if (!terreno) return setEstado({ fase: "no-encontrado" });

        const municipio = nombreMunicipio(terreno);
        let respuesta = await obtenerPlanGuardado(loteId);
        if (!sigue()) return;
        if (!respuesta) {
          setEstado({ fase: "calculando" });
          respuesta = await calcularPlan(loteId);
        }
        const plan = aPlanRotacion(respuesta, { lote: aLoteOpcion(terreno), lotes, municipio: await municipio });
        if (sigue()) setEstado({ fase: "listo", plan });
      } catch (e) {
        if (!sigue()) return;
        const error = comoErrorApi(e);
        setEstado(error.codigo === "RECOMENDACION_TERRENO_NO_ENCONTRADO" ? { fase: "no-encontrado" } : { fase: "error", error });
      }
    })();
  }, [loteId, intento]);

  const reintentar = useCallback(() => setIntento((n) => n + 1), []);

  /** Elige el ciclo: se marca de una vez y, si el backend falla, vuelve al anterior y dice por qué. */
  const elegir = useCallback(
    async (rotacionId: string) => {
      if (estado.fase !== "listo" || eligiendo) return;
      const anterior = estado.plan.seleccion;
      const rotacion = estado.plan.rotaciones.find((r) => r.id === rotacionId);
      if (!rotacion) return;
      setErrorSeleccion(null);
      setEligiendo(rotacionId);
      const conSeleccion = (seleccion: PlanRotacion["seleccion"]) =>
        setEstado((e) => (e.fase === "listo" ? { fase: "listo", plan: { ...e.plan, seleccion } } : e));
      conSeleccion({ rotacionId, secuencia: rotacion.secuencia, elegidaEn: new Date().toISOString() });
      try {
        conSeleccion(await elegirRotacion(loteId, estado.plan.planId, rotacionId));
      } catch (e) {
        conSeleccion(anterior);
        setErrorSeleccion(`No pudimos guardar su elección: ${comoErrorApi(e).mensaje}`);
      } finally {
        setEligiendo(null);
      }
    },
    [estado, eligiendo, loteId],
  );

  return { estado, reintentar, elegir, eligiendo, errorSeleccion };
}
