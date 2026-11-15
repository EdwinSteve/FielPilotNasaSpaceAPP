// Apariencia de cada nivel de alerta (los colores del IDEAM).
import type { AlertaResumen, NivelAlerta } from "../tipos";

export const NIVELES: Record<
  NivelAlerta,
  { etiqueta: string; aviso: string; texto: string; fondo: string; icono: string; iconoPequeno: string }
> = {
  roja: {
    etiqueta: "ALERTA ROJA",
    aviso: "AVISO ROJO",
    texto: "text-roja-texto",
    fondo: "bg-roja-fondo",
    icono: "alert-roja",
    // El diseño solo trae la variante de 16 px en naranja; las demás escalan la de 20.
    iconoPequeno: "alert-roja",
  },
  naranja: {
    etiqueta: "ALERTA NARANJA",
    aviso: "AVISO NARANJA",
    texto: "text-naranja-texto",
    fondo: "bg-naranja-fondo",
    icono: "alert-naranja",
    iconoPequeno: "alert-naranja-16",
  },
  amarilla: {
    etiqueta: "ALERTA AMARILLA",
    aviso: "AVISO AMARILLO",
    texto: "text-amarilla-texto",
    fondo: "bg-amarilla-fondo",
    icono: "alert-amarilla",
    iconoPequeno: "alert-amarilla",
  },
};

/** "ALERTA NARANJA · IDEAM" para las oficiales; "AVISO AMARILLO · NASA POWER" para las que calcula la app. */
export function etiquetaDe(alerta: AlertaResumen): string {
  const nivel = NIVELES[alerta.nivel];
  return `${alerta.oficial ? nivel.etiqueta : nivel.aviso} · ${alerta.fuente}`;
}
