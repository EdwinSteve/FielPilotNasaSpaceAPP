import { beforeEach, describe, expect, it, vi } from "vitest";
import type { RespuestaPlanRotacion } from "./tipos";

const peticion = vi.fn();
vi.mock("@/compartido/api/http", () => ({ peticion: (...a: unknown[]) => peticion(...a) }));
vi.mock("@/modulos/territorio", () => ({
  listarMunicipios: async () => [{ codigo: "41001", nombre: "Neiva" }],
}));

const { aLoteOpcion, aPlanRotacion, obtenerPlanRotacion, obtenerPlanGuardado } = await import("./api");

const TERRENO = {
  id: "t-1",
  nombre: "El Mirador",
  municipio: "41001",
  departamento: "41",
  riego: false,
  area_ha: 4.2,
  centroide: { lat: 2.9275, lon: -75.2875 },
  vertices: [],
  poligono_geojson: "",
  creado_en: "2026-11-15T10:00:00Z",
};

const PLAN: RespuestaPlanRotacion = {
  id: "plan-1",
  terreno_id: "t-1",
  recomendacion_id: "rec-1",
  creado_en: "2026-11-15T15:00:00Z",
  nombre_terreno: "El Mirador",
  area_ha: 4.2,
  motor: "bf",
  fuente_historico: "NASA POWER 1981-2026 (años pasados del terreno)",
  rotaciones: [
    {
      id: "crotalaria-maiz-yuca",
      grado: "A+",
      secuencia: ["Crotalaria (abono verde)", "Maíz", "Yuca"],
      temporadas: [
        { cultivo: "crotalaria", nombre: "Crotalaria (abono verde)", siembra: "2026-11-16", cosecha: "2027-01-30", prob_exito: 1, fuente: "pronostico" },
        { cultivo: "maiz", nombre: "Maíz", siembra: "2027-02-14", cosecha: "2027-07-04", prob_exito: 0.957, fuente: "historico" },
      ],
      puntaje: 1,
      frecuencia_exito: 10,
      agua_ahorrada: "12.940 m³",
      agua_ahorrada_m3: 12940,
      aporte_nitrogeno: "Antes de Maíz va Crotalaria (abono verde), que fija unos 115 kg de nitrógeno por hectárea.",
    },
  ],
  ranking: [
    { id: "yuca", nombre: "Yuca", puntaje: 0.895, apto: true, razon_no_apto: null },
    { id: "papa", nombre: "Papa", puntaje: 0, apto: false, razon_no_apto: "Se cultiva entre 2000 y 3200 m de altitud; su terreno está a 446 m." },
  ],
  lluvias: {
    fuente: "NASA POWER · años parecidos según El Niño (ONI 2,2)",
    meses: [{ mes: "Diciembre", lluvia_mm: 149, nivel: "normal", nota: null }],
  },
  seleccion: { plan_id: "plan-1", rotacion_id: "crotalaria-maiz-yuca", secuencia: ["Crotalaria (abono verde)", "Maíz", "Yuca"], elegida_en: "2026-11-15T16:00:00Z" },
};

beforeEach(() => peticion.mockReset());

describe("mapeo del plan del backend a la pantalla", () => {
  it("terreno → opción del selector con el área en español", () => {
    expect(aLoteOpcion(TERRENO)).toEqual({ id: "t-1", nombre: "El Mirador", area: "4,2 ha" });
  });

  it("convierte rotaciones, ranking, lluvias y selección", () => {
    const lote = aLoteOpcion(TERRENO);
    const p = aPlanRotacion(PLAN, { lote, lotes: [lote], municipio: "Neiva" });
    expect(p.planId).toBe("plan-1");
    expect(p.finca).toEqual({ nombre: "El Mirador", municipio: "Neiva" });
    expect(p.rotaciones[0]).toMatchObject({ grado: "A+", frecuenciaExito: 10, aguaAhorrada: "12.940 m³" });
    expect(p.rotaciones[0].temporadas[1]).toEqual({
      cultivo: "maiz", nombre: "Maíz", siembra: "2027-02-14", cosecha: "2027-07-04", probExito: 0.957, fuente: "historico",
    });
    expect(p.ranking[1]).toEqual({ id: "papa", nombre: "Papa", puntaje: 0, apto: false, razonNoApto: PLAN.ranking[1].razon_no_apto });
    expect(p.lluvias.meses[0]).toEqual({ mes: "Diciembre", lluviaMm: 149, nivel: "normal", nota: null });
    expect(p.seleccion).toEqual({ rotacionId: "crotalaria-maiz-yuca", secuencia: PLAN.seleccion!.secuencia, elegidaEn: "2026-11-15T16:00:00Z" });
  });
});

describe("obtenerPlanRotacion", () => {
  it("un lote que no es del productor da null (la pantalla dice 'no encontrado')", async () => {
    peticion.mockResolvedValueOnce([TERRENO]); // GET /fincas/terrenos
    expect(await obtenerPlanRotacion("otro")).toBeNull();
    expect(peticion).toHaveBeenCalledTimes(1);
  });

  it("sin plan guardado lo calcula con el modelo", async () => {
    peticion
      .mockResolvedValueOnce([TERRENO]) // GET /fincas/terrenos
      .mockRejectedValueOnce({ codigo: "ROTACION_SIN_CALCULAR", mensaje: "..." }) // GET rotaciones
      .mockResolvedValueOnce(PLAN); // POST rotaciones
    const p = await obtenerPlanRotacion("t-1");
    expect(p?.planId).toBe("plan-1");
    expect(p?.finca.municipio).toBe("Neiva");
    expect(peticion.mock.calls.map((c) => `${c[0]} ${c[1]}`)).toEqual([
      "GET /fincas/terrenos",
      "GET /lotes/t-1/rotaciones",
      "POST /lotes/t-1/rotaciones",
    ]);
    expect(peticion.mock.calls[2][2]).toEqual({ autenticada: true });
  });

  it("los demás errores (fuente caída, modelo no disponible) llegan a la pantalla", async () => {
    peticion.mockRejectedValueOnce({ codigo: "RECOMENDACION_FUENTE_CAIDA", mensaje: "No pudimos consultar NASA POWER." });
    await expect(obtenerPlanGuardado("t-1")).rejects.toMatchObject({ codigo: "RECOMENDACION_FUENTE_CAIDA" });
  });
});
