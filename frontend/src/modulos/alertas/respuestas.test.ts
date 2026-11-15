import { describe, expect, it } from "vitest";
import {
  formatearConsulta,
  formatearUbicacion,
  interpretarRios,
  interpretarSequia,
  tieneAlerta,
} from "./respuestas";

const cuerpo = { lat: 2.9273, lon: -75.2819, consultado_en: "2026-11-15T15:32:00Z" };

describe("interpretarRios (GET /alerts/flood)", () => {
  it("lee una alerta naranja con su subzona", () => {
    const r = interpretarRios({
      estado: 200,
      cuerpo: { ...cuerpo, nivel: "NARANJA", subzona: "Río Fortalecillas y otros", tipo: "Hidrológica" },
    });
    expect(r).toEqual({
      estado: "ok",
      datos: {
        nivel: "NARANJA",
        subzona: "Río Fortalecillas y otros",
        tipo: "Hidrológica",
        lat: 2.9273,
        lon: -75.2819,
        consultadoEn: "2026-11-15T15:32:00Z",
      },
    });
  });

  it("SIN_ALERTA no muestra subzona ni tipo aunque lleguen", () => {
    const r = interpretarRios({ estado: 200, cuerpo: { ...cuerpo, nivel: "SIN_ALERTA", subzona: "X", tipo: "Y" } });
    expect(r.estado === "ok" && [r.datos.subzona, r.datos.tipo]).toEqual([null, null]);
  });

  it.each([
    [503, "SIN_CONEXION"],
    [500, "SIN_CONEXION"],
    [502, "NIVEL_DESCONOCIDO"],
    [400, "UBICACION_INVALIDA"],
  ])("HTTP %i → %s", (estado, fallo) => {
    expect(interpretarRios({ estado, cuerpo: null })).toEqual({ estado: "error", fallo });
  });

  it("un 200 con un nivel que no existe es una respuesta no reconocida", () => {
    expect(interpretarRios({ estado: 200, cuerpo: { ...cuerpo, nivel: "MORADA" } })).toEqual({
      estado: "error",
      fallo: "NIVEL_DESCONOCIDO",
    });
  });

  it("un 200 sin ubicación tampoco se acepta", () => {
    expect(interpretarRios({ estado: 200, cuerpo: { nivel: "NORMAL" } }).estado).toBe("error");
  });
});

describe("interpretarSequia (GET /alerts/drought)", () => {
  it("acepta los cuatro niveles y rechaza SIN_ALERTA", () => {
    for (const nivel of ["NORMAL", "AMARILLA", "NARANJA", "ROJA"]) {
      expect(interpretarSequia({ estado: 200, cuerpo: { ...cuerpo, nivel } }).estado).toBe("ok");
    }
    expect(interpretarSequia({ estado: 200, cuerpo: { ...cuerpo, nivel: "SIN_ALERTA" } })).toEqual({
      estado: "error",
      fallo: "NIVEL_DESCONOCIDO",
    });
  });
});

describe("tieneAlerta", () => {
  const ok = <T>(datos: T) => ({ estado: "ok" as const, datos });
  const rios = (nivel: "SIN_ALERTA" | "NORMAL" | "AMARILLA") =>
    ok({ nivel, subzona: null, tipo: null, lat: 0, lon: 0, consultadoEn: "" });
  const sequia = (nivel: "NORMAL" | "AMARILLA") => ok({ nivel, lat: 0, lon: 0, consultadoEn: "" });

  it("cuenta amarilla o más en ríos, o cualquier nivel de sequía", () => {
    expect(tieneAlerta({ rios: rios("NORMAL"), sequia: sequia("NORMAL") })).toBe(false);
    expect(tieneAlerta({ rios: rios("SIN_ALERTA"), sequia: sequia("NORMAL") })).toBe(false);
    expect(tieneAlerta({ rios: rios("AMARILLA"), sequia: sequia("NORMAL") })).toBe(true);
    expect(tieneAlerta({ rios: rios("NORMAL"), sequia: sequia("AMARILLA") })).toBe(true);
    expect(tieneAlerta({ rios: { estado: "cargando" }, sequia: { estado: "error", fallo: "SIN_CONEXION" } })).toBe(false);
  });
});

describe("formatos", () => {
  it("ubicación como en el diseño", () => {
    expect(formatearUbicacion(2.9273, -75.2819)).toBe("2,9273° N, 75,2819° O");
    expect(formatearUbicacion(-4.2, 70.1)).toBe("4,2000° S, 70,1000° E");
  });

  it("hora de la consulta: hoy u otro día (hora de Colombia)", () => {
    const ahora = new Date("2026-11-15T20:00:00Z");
    expect(formatearConsulta("2026-11-15T15:32:00Z", ahora)).toBe("Hoy, 10:32 a. m.");
    expect(formatearConsulta("2026-11-14T15:32:00Z", ahora)).toBe("14 de nov, 10:32 a. m.");
    expect(formatearConsulta("no es fecha", ahora)).toBe("—");
  });
});
