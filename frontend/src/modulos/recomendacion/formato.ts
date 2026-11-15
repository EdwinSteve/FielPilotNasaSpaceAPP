// Cómo se le dicen al agricultor las cifras del modelo.
import type { PlanCiclo } from "./tipos";

const FECHA = new Intl.DateTimeFormat("es-CO", { day: "numeric", month: "short", timeZone: "UTC" });
const FECHA_LARGA = new Intl.DateTimeFormat("es-CO", { day: "numeric", month: "long", year: "numeric", timeZone: "UTC" });

/** "2026-12-06" -> "6 dic." (las fechas del modelo son días, sin hora: se leen en UTC). */
export function fechaCorta(iso: string): string {
  return FECHA.format(new Date(`${iso}T00:00:00Z`));
}

export function fechaLarga(iso: string): string {
  return FECHA_LARGA.format(new Date(`${iso}T00:00:00Z`));
}

/** 0.472 -> "47 %" del rendimiento potencial. */
export function porcentaje(x: number): string {
  return `${Math.round(x * 100)} %`;
}

/** Probabilidad de éxito como "8 de cada 10 años", la forma en que la dice el agricultor. */
export function deCadaDiez(p: number): string {
  const n = Math.round(p * 10);
  if (n === 0) return p > 0 ? "menos de 1 de cada 10 años" : "casi nunca";
  return `${n} de cada 10 años`;
}

export function fraseEnso(p: PlanCiclo["pronostico"]): { titulo: string; detalle: string } {
  const fuerte = Math.abs(p.oni) >= 1.5 ? " fuerte" : "";
  if (p.fase_enso === "nino")
    return {
      titulo: `El Niño${fuerte} en curso`,
      detalle: "En la región andina y el Caribe suele traer menos lluvia y más calor de lo normal.",
    };
  if (p.fase_enso === "nina")
    return {
      titulo: `La Niña${fuerte} en curso`,
      detalle: "Suele traer más lluvia de lo normal: atención a encharcamientos y enfermedades.",
    };
  return { titulo: "Condiciones neutrales", detalle: "Sin El Niño ni La Niña: se espera un clima cercano a lo normal." };
}
