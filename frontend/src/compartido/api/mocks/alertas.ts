// Datos de ejemplo de las alertas (los mismos del diseño en Figma: Alertas IDEAM).
// Simulan las respuestas HTTP de /alerts/flood y /alerts/drought, incluidos los errores.
import { lotesMock } from "@/compartido/api/mocks/rotacion";
import type { LoteUbicado, RespuestaHttp, Simulacion } from "@/modulos/alertas/tipos";

const UBICACIONES: Record<string, { lat: number; lon: number }> = {
  "el-mirador": { lat: 2.9273, lon: -75.2819 },
  "la-vega": { lat: 2.8651, lon: -75.3262 },
  "el-guadual": { lat: 2.9804, lon: -75.2203 },
};

export const lotesUbicadosMock: LoteUbicado[] = lotesMock.map((l) => ({ ...l, ...UBICACIONES[l.id] }));

const RIOS: Record<string, { nivel: string; subzona: string | null; tipo: string | null }> = {
  "el-mirador": { nivel: "NARANJA", subzona: "Río Fortalecillas y otros", tipo: "Hidrológica" },
  "la-vega": { nivel: "AMARILLA", subzona: "Río Neiva", tipo: "Hidrológica" },
  "el-guadual": { nivel: "SIN_ALERTA", subzona: null, tipo: null },
};

const SEQUIA: Record<string, string> = {
  "el-mirador": "AMARILLA",
  "la-vega": "NORMAL",
  "el-guadual": "NORMAL",
};

const ERRORES: Record<string, { code: string; message: string }> = {
  400: { code: "UBICACION_FUERA_DE_COLOMBIA", message: "La ubicación está fuera de Colombia." },
  502: { code: "NIVEL_DESCONOCIDO", message: "La fuente envió un nivel de alerta desconocido." },
  503: { code: "FUENTE_NO_DISPONIBLE", message: "La fuente no respondió." },
};

const error = (estado: number): RespuestaHttp => ({ estado, cuerpo: ERRORES[estado] });

const pausa = () => new Promise((r) => setTimeout(r, 350));

export async function respuestaRiosMock(lote: LoteUbicado, simular?: Simulacion): Promise<RespuestaHttp> {
  await pausa();
  if (simular === "400") return error(400);
  if (simular === "rios-503") return error(503);
  if (simular === "rios-502") return error(502);
  const rios = RIOS[lote.id] ?? RIOS["el-guadual"];
  return {
    estado: 200,
    cuerpo: { ...rios, lat: lote.lat, lon: lote.lon, consultado_en: new Date().toISOString() },
  };
}

export async function respuestaSequiaMock(lote: LoteUbicado, simular?: Simulacion): Promise<RespuestaHttp> {
  await pausa();
  if (simular === "400") return error(400);
  if (simular === "sequia-503") return error(503);
  return {
    estado: 200,
    cuerpo: {
      nivel: SEQUIA[lote.id] ?? "NORMAL",
      lat: lote.lat,
      lon: lote.lon,
      consultado_en: new Date().toISOString(),
    },
  };
}
