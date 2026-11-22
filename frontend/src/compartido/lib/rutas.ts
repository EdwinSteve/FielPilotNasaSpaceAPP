// Rutas de la app que se arman en más de un módulo.

/** Ruta de "Cultivos y rotación": la del lote dado, o /cultivos para que se abra el primer terreno. */
export function rutaCultivos(loteId?: string): string {
  return loteId ? `/lotes/${encodeURIComponent(loteId)}/rotacion` : "/cultivos";
}
