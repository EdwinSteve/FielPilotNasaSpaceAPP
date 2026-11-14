// Apariencia de cada nivel de alerta del IDEAM.
import type { NivelAlerta } from "../tipos";

export const NIVELES: Record<
  NivelAlerta,
  { etiqueta: string; texto: string; fondo: string; icono: string; iconoPequeno: string }
> = {
  naranja: {
    etiqueta: "ALERTA NARANJA",
    texto: "text-naranja-texto",
    fondo: "bg-naranja-fondo",
    icono: "alert-naranja",
    iconoPequeno: "alert-naranja-16",
  },
  amarilla: {
    etiqueta: "ALERTA AMARILLA",
    texto: "text-amarilla-texto",
    fondo: "bg-amarilla-fondo",
    icono: "alert-amarilla",
    // El diseño no trae la variante de 16 px en amarillo; se escala la de 20.
    iconoPequeno: "alert-amarilla",
  },
};
