// Geometría del contorno del terreno: las mismas reglas y fórmulas del backend
// (co.surco.fincas.dominio.Poligono), para avisar mientras el productor dibuja.
import type { Vertice } from "./tipos";

export const MINIMO_VERTICES = 3;
export const MAXIMO_VERTICES = 200;
export const AREA_MINIMA_HA = 0.01;
export const AREA_MAXIMA_HA = 20_000;

const RADIO_TIERRA_M = 6_371_008.8;
const GRADO = Math.PI / 180;

export function enColombia(v: Vertice): boolean {
  return v.lat >= -4.3 && v.lat <= 13.6 && v.lon >= -82.0 && v.lon <= -66.8;
}

/** Área (ha) y centroide en una proyección local equirectangular, como el backend. */
export function medir(vertices: Vertice[]): { areaHa: number; centroide: Vertice } | null {
  const n = vertices.length;
  if (n < MINIMO_VERTICES) return null;
  const lat0 = vertices.reduce((s, v) => s + v.lat, 0) / n;
  const lon0 = vertices.reduce((s, v) => s + v.lon, 0) / n;
  const ex = GRADO * RADIO_TIERRA_M * Math.cos(lat0 * GRADO);
  const ey = GRADO * RADIO_TIERRA_M;
  let doble = 0;
  let cx = 0;
  let cy = 0;
  for (let i = 0; i < n; i++) {
    const a = vertices[i];
    const b = vertices[(i + 1) % n];
    const xa = (a.lon - lon0) * ex;
    const ya = (a.lat - lat0) * ey;
    const xb = (b.lon - lon0) * ex;
    const yb = (b.lat - lat0) * ey;
    const cruz = xa * yb - xb * ya;
    doble += cruz;
    cx += (xa + xb) * cruz;
    cy += (ya + yb) * cruz;
  }
  const centroide = doble === 0 ? { lat: lat0, lon: lon0 } : { lat: lat0 + cy / (3 * doble) / ey, lon: lon0 + cx / (3 * doble) / ex };
  return { areaHa: Math.abs(doble) / 2 / 10_000, centroide };
}

function orientacion(p: Vertice, q: Vertice, r: Vertice): number {
  return (q.lon - p.lon) * (r.lat - p.lat) - (q.lat - p.lat) * (r.lon - p.lon);
}

function sobre(p: Vertice, q: Vertice, r: Vertice): boolean {
  return (
    Math.min(p.lon, q.lon) <= r.lon && r.lon <= Math.max(p.lon, q.lon) && Math.min(p.lat, q.lat) <= r.lat && r.lat <= Math.max(p.lat, q.lat)
  );
}

function segmentosSeTocan(a: Vertice, b: Vertice, c: Vertice, d: Vertice): boolean {
  const d1 = orientacion(c, d, a);
  const d2 = orientacion(c, d, b);
  const d3 = orientacion(a, b, c);
  const d4 = orientacion(a, b, d);
  if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) return true;
  return (d1 === 0 && sobre(c, d, a)) || (d2 === 0 && sobre(c, d, b)) || (d3 === 0 && sobre(a, b, c)) || (d4 === 0 && sobre(a, b, d));
}

/** ¿Algún par de lados no vecinos se toca? */
export function seCruza(v: Vertice[]): boolean {
  const n = v.length;
  for (let i = 0; i < n; i++) {
    for (let j = i + 1; j < n; j++) {
      const vecinos = j === i + 1 || (i === 0 && j === n - 1);
      if (!vecinos && segmentosSeTocan(v[i], v[(i + 1) % n], v[j], v[(j + 1) % n])) return true;
    }
  }
  return false;
}

/** El primer problema del contorno, en palabras del agricultor; null si se puede guardar. */
export function problemaDelContorno(vertices: Vertice[]): string | null {
  if (vertices.length < MINIMO_VERTICES) {
    const faltan = MINIMO_VERTICES - vertices.length;
    return `Toque el mapa en cada esquina del terreno (${faltan === 1 ? "falta 1 punto" : `faltan ${faltan} puntos`}).`;
  }
  if (vertices.length > MAXIMO_VERTICES) return `Marque solo las esquinas (máximo ${MAXIMO_VERTICES} puntos).`;
  if (!vertices.every(enColombia)) return "El terreno debe estar en Colombia.";
  if (seCruza(vertices)) return "Los lados se cruzan: marque las esquinas en orden, siguiendo el borde.";
  const m = medir(vertices);
  if (m && m.areaHa < AREA_MINIMA_HA) return "El terreno marcado es muy pequeño (menos de 100 m²).";
  if (m && m.areaHa > AREA_MAXIMA_HA) return "El terreno marcado es demasiado grande (más de 20.000 hectáreas).";
  return null;
}

/** 0,85 ha · 2.350 m² — como lo dice el agricultor. */
export function formatoArea(areaHa: number): string {
  if (areaHa < 1) return `${Math.round(areaHa * 10_000).toLocaleString("es-CO")} m²`;
  const decimales = areaHa < 10 ? 2 : areaHa < 100 ? 1 : 0;
  return `${areaHa.toLocaleString("es-CO", { maximumFractionDigits: decimales, minimumFractionDigits: 0 })} ha`;
}
