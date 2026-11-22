import { describe, expect, it } from "vitest";
import { rutaCultivos } from "./rutas";

describe("rutaCultivos", () => {
  it("abre la rotación del lote dado", () => {
    expect(rutaCultivos("cf2524b0-b51d-4f65-b186-ad2aea656d18")).toBe("/lotes/cf2524b0-b51d-4f65-b186-ad2aea656d18/rotacion");
  });

  it("sin lote, va a /cultivos para abrir el primer terreno del productor", () => {
    expect(rutaCultivos()).toBe("/cultivos");
    expect(rutaCultivos("")).toBe("/cultivos");
  });
});
