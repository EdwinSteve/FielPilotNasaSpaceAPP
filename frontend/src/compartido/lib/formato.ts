// Formato de números, unidades (m³, kg N/ha, °C) y fechas en español.

const decimales2 = new Intl.NumberFormat("es-CO", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** 0.83 → "0,83" (recortado entre 0 y 1). */
export function formatearPuntaje(puntaje: number): string {
  return decimales2.format(Math.max(0, Math.min(1, puntaje)));
}
