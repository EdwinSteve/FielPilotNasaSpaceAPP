import { beforeEach, describe, expect, it, vi } from "vitest";

const peticion = vi.fn();
vi.mock("@/compartido/api/http", () => ({ peticion: (...a: unknown[]) => peticion(...a) }));

const { aLoteUbicado, consultarRios, consultarSequia, obtenerLotesUbicados } = await import("./api");

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

const LOTE = aLoteUbicado(TERRENO);
const UBICACION = { lat: 2.9275, lon: -75.2875, consultado_en: "2026-11-15T15:32:00Z" };

beforeEach(() => {
  peticion.mockReset();
});

describe("lotes de la pantalla de alertas", () => {
  it("ubica cada terreno en su centroide", async () => {
    peticion.mockResolvedValue([TERRENO]);
    expect(await obtenerLotesUbicados()).toEqual([LOTE]);
    expect(LOTE).toMatchObject({ id: "t-1", nombre: "El Mirador", lat: 2.9275, lon: -75.2875 });
    expect(peticion).toHaveBeenCalledWith("GET", "/fincas/terrenos", { autenticada: true });
  });
});

describe("consultarRios y consultarSequia", () => {
  it("piden /alerts/flood y /alerts/drought con la ubicación del lote", async () => {
    peticion.mockResolvedValueOnce({ ...UBICACION, nivel: "NARANJA", subzona: "Río Neiva", tipo: "Hidrológica" });
    peticion.mockResolvedValueOnce({ ...UBICACION, nivel: "NORMAL" });

    const rios = await consultarRios(LOTE);
    const sequia = await consultarSequia(LOTE);

    expect(peticion).toHaveBeenNthCalledWith(1, "GET", "/alerts/flood?lat=2.9275&lon=-75.2875", { autenticada: true });
    expect(peticion).toHaveBeenNthCalledWith(2, "GET", "/alerts/drought?lat=2.9275&lon=-75.2875", {
      autenticada: true,
    });
    expect(rios).toMatchObject({ estado: "ok", datos: { nivel: "NARANJA", subzona: "Río Neiva" } });
    expect(sequia).toMatchObject({ estado: "ok", datos: { nivel: "NORMAL" } });
  });

  it.each([
    ["UBICACION_FUERA_DE_COLOMBIA", "UBICACION_INVALIDA"],
    ["NIVEL_DESCONOCIDO", "NIVEL_DESCONOCIDO"],
    ["FUENTE_NO_DISPONIBLE", "SIN_CONEXION"],
    ["RED_SIN_CONEXION", "SIN_CONEXION"],
  ])("el error %s del backend se muestra como %s", async (codigo, fallo) => {
    peticion.mockRejectedValue({ codigo, mensaje: "..." });
    expect(await consultarRios(LOTE)).toEqual({ estado: "error", fallo });
  });

  it("?simular fuerza el error sin llamar al backend en esa consulta", async () => {
    peticion.mockResolvedValue({ ...UBICACION, nivel: "NORMAL" });
    expect(await consultarRios(LOTE, "rios-502")).toEqual({ estado: "error", fallo: "NIVEL_DESCONOCIDO" });
    expect(peticion).not.toHaveBeenCalled();
    expect(await consultarSequia(LOTE, "rios-502")).toMatchObject({ estado: "ok" });
  });
});
