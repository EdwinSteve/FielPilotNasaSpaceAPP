import { describe, expect, it } from "vitest";
import { formatoArea, medir, problemaDelContorno, seCruza } from "./geometria";
import type { Vertice } from "./tipos";

const cuadrado = (lat: number, lon: number, g: number): Vertice[] => [
  { lat, lon },
  { lat, lon: lon + g },
  { lat: lat + g, lon: lon + g },
  { lat: lat + g, lon },
];

describe("geometría del terreno (mismas fórmulas que el backend)", () => {
  it("mide área y centroide de un cuadrado", () => {
    const m = medir(cuadrado(4.6, -74.08, 0.001))!;
    const lado = (0.001 * Math.PI) / 180 * 6_371_008.8;
    const esperada = (lado * lado * Math.cos((4.6005 * Math.PI) / 180)) / 10_000;
    expect(m.areaHa).toBeCloseTo(esperada, 3);
    expect(m.centroide.lat).toBeCloseTo(4.6005, 7);
    expect(m.centroide.lon).toBeCloseTo(-74.0795, 7);
  });

  it("detecta lados cruzados", () => {
    const corbatin: Vertice[] = [
      { lat: 5.0, lon: -75.5 },
      { lat: 5.002, lon: -75.498 },
      { lat: 5.0, lon: -75.498 },
      { lat: 5.002, lon: -75.5 },
    ];
    expect(seCruza(corbatin)).toBe(true);
    expect(seCruza(cuadrado(5, -75.5, 0.002))).toBe(false);
    expect(problemaDelContorno(corbatin)).toMatch(/se cruzan/);
  });

  it("explica qué falta", () => {
    expect(problemaDelContorno([])).toMatch(/faltan 3 puntos/);
    expect(problemaDelContorno(cuadrado(5, -75.5, 0.002).slice(0, 2))).toMatch(/falta 1 punto/);
    expect(problemaDelContorno(cuadrado(40.4, -3.7, 0.002))).toMatch(/Colombia/);
    expect(problemaDelContorno(cuadrado(5, -75.5, 0.002))).toBeNull();
  });

  it("formatea el área como la dice el agricultor", () => {
    expect(formatoArea(0.235)).toBe("2.350 m²");
    expect(formatoArea(4.2)).toBe("4,2 ha");
  });
});
