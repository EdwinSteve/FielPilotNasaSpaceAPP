// La lluvia de un mes dicha en palabras, con cuántas gotas dibujar (0 = un sol).
import type { LluviaMensual } from "../tipos";

export interface LluviaEnPalabras {
  texto: string;
  gotas: 0 | 1 | 2 | 3;
  fondo: string;
}

export function lluviaEnPalabras({ nivel, lluviaMm }: Pick<LluviaMensual, "nivel" | "lluviaMm">): LluviaEnPalabras {
  if (nivel === "seco") return { texto: "Poca lluvia", gotas: 0, fondo: "bg-amarilla-fondo" };
  if (nivel === "normal") return { texto: "Lluvia normal", gotas: lluviaMm >= 100 ? 2 : 1, fondo: "bg-fondo" };
  return { texto: lluviaMm >= 180 ? "Muchas lluvias" : "Buenas lluvias", gotas: 3, fondo: "bg-verde-100" };
}
