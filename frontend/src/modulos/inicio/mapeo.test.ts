import { describe, expect, it } from "vitest";
import { aDatosInicio } from "./mapeo";
import type { ResumenClimaApi } from "./tipos";

// Respuesta real de GET /clima/resumen?municipio=41001 (recortada a 3 días)
const api: ResumenClimaApi = {
  ubicacion: { municipio: "Neiva", departamento: "Huila", lat: 2.9273, lon: -75.2819, altitud_m: 456 },
  actualizado: "2026-11-15T10:45:00-05:00",
  actual: {
    temperatura: 34.1,
    sensacion: 38.1,
    descripcion: "Mayormente despejado",
    icono: "sun",
    humedad: 36,
    viento_kmh: 3.3,
    viento_rumbo: "O",
    rafagas_kmh: 17.6,
    lluvia_dia_mm: 1.2,
    lluvia_semana_mm: 85.6,
  },
  satelite: {
    radiacion_kwh: 4.8,
    radiacion_fecha: "2026-11-10",
    humedad_suelo_pct: 71,
    humedad_suelo_fecha: "2026-11-12",
  },
  pronostico: [
    { fecha: "2026-11-15", descripcion: "Llovizna", icono: "rain", temp_max: 35.7, temp_min: 24.2, lluvia_mm: 1.5, prob_lluvia: 78, indice_uv: 8.65, lluvia_fuerte: false },
    { fecha: "2026-11-16", descripcion: "Chubascos", icono: "rain", temp_max: 34, temp_min: 23.3, lluvia_mm: 6.6, prob_lluvia: 94, indice_uv: 8.85, lluvia_fuerte: false },
    { fecha: "2026-11-17", descripcion: "Lluvia fuerte", icono: "rain", temp_max: 31.9, temp_min: 22.5, lluvia_mm: 38.4, prob_lluvia: 76, indice_uv: 8.35, lluvia_fuerte: true },
  ],
  avisos: [
    { tipo: "RIOS", nivel: "NARANJA", titulo: "Los ríos y quebradas pueden crecer pronto", mensaje: "Prepárese.", zona: "Río Neiva", fuente: "IDEAM", oficial: true },
    { tipo: "LLUVIA_FUERTE", nivel: "AMARILLA", titulo: "Lluvia fuerte el martes", mensaje: "Se esperan cerca de 38 mm de lluvia.", zona: null, fuente: "Open-Meteo", oficial: false },
  ],
  fuentes: ["Open-Meteo", "NASA POWER", "IDEAM"],
};

// Domingo 15 de noviembre de 2026, 10:50 a. m. en Colombia
const ahora = new Date("2026-11-15T15:50:00Z");

describe("aDatosInicio", () => {
  const datos = aDatosInicio(api, ahora);

  it("saluda y escribe la fecha en hora de Colombia", () => {
    expect(datos.saludo).toBe("Buenos días");
    expect(datos.fecha).toEqual({
      larga: "Domingo 15 de noviembre de 2026",
      media: "Dom. 15 nov. 2026",
      corta: "Dom. 15 nov.",
    });
    expect(aDatosInicio(api, new Date("2026-11-15T19:00:00Z")).saludo).toBe("Buenas tardes");
    // 9:30 p. m. del domingo en Colombia ya es lunes en UTC
    const noche = aDatosInicio(api, new Date("2026-11-16T02:30:00Z"));
    expect(noche.saludo).toBe("Buenas noches");
    expect(noche.fecha.corta).toBe("Dom. 15 nov.");
  });

  it("describe la ubicación", () => {
    expect(datos.ubicacion).toEqual({
      nombre: "Neiva",
      departamento: "Huila",
      coordenadas: "2,9273° N, 75,2819° O",
      altitud: "456 m s. n. m.",
    });
  });

  it("arma el banner del clima", () => {
    expect(datos.clima.temperatura).toBe("34°");
    expect(datos.clima.condicion).toBe("Mayormente despejado");
    expect(datos.clima.detalle).toBe("Sensación térmica 38 °C · Máx. 36° / Mín. 24°");
    expect(datos.clima.lluviaHoy).toBe("Probabilidad de lluvia hoy: 78 % · se esperan 1,5 mm");
    // El espacio de "a. m." cambia entre versiones de Node y navegadores
    expect(datos.clima.fuentes).toMatch(/^Fuentes: Open-Meteo · NASA POWER · IDEAM — Actualizado hoy, 10:45\sa\.\sm\.$/);
  });

  it("formatea las seis métricas con su fuente", () => {
    expect(datos.metricas.map((m) => [m.nombre, m.valor, m.unidad, m.fuente])).toEqual([
      ["Temperatura del aire", "34", "°C", "Open-Meteo"],
      ["Precipitación (24 h)", "1,2", "mm", "Open-Meteo"],
      ["Humedad relativa", "36", "%", "Open-Meteo"],
      ["Viento", "3", "km/h", "Open-Meteo"],
      ["Radiación solar", "4,8", "kWh/m²", "NASA POWER"],
      ["Humedad del suelo", "71", "%", "NASA POWER"],
    ]);
    expect(datos.metricas[1].detalle).toBe("Acumulado 7 días: 86 mm");
    expect(datos.metricas[3].detalle).toBe("Viene del O · ráfagas 18 km/h");
    expect(datos.metricas[4].detalle).toBe("Total del 10 nov.");
    expect(datos.metricas[5].detalle).toBe("Zona de raíces · 12 nov.");
  });

  it("nombra los días del pronóstico", () => {
    expect(datos.pronostico).toEqual([
      { dia: "Hoy", icono: "rain", temperaturas: "36° / 24°", lluviaMm: 2, lluviaFuerte: false },
      { dia: "Lun", icono: "rain", temperaturas: "34° / 23°", lluviaMm: 7, lluviaFuerte: false },
      { dia: "Mar", icono: "rain", temperaturas: "32° / 23°", lluviaMm: 38, lluviaFuerte: true },
    ]);
  });

  it("destaca el aviso más grave y distingue las alertas oficiales", () => {
    expect(datos.alertasPendientes).toBe(2);
    expect(datos.alertaDestacada).toEqual({
      nivel: "naranja",
      titulo: "Los ríos y quebradas pueden crecer pronto",
      detalle: "Río Neiva · Prepárese.",
      fuente: "IDEAM",
      oficial: true,
    });
    expect(datos.alertas[1].oficial).toBe(false);
  });

  it("sin NASA POWER ni avisos, lo dice en vez de inventar", () => {
    const sinNada = aDatosInicio({ ...api, satelite: null, avisos: [], fuentes: ["Open-Meteo"] }, ahora);
    expect(sinNada.alertaDestacada).toBeNull();
    expect(sinNada.sinAlertas).toContain("no pudimos consultar las alertas del IDEAM");
    expect(sinNada.metricas[4].valor).toBe("—");
    expect(sinNada.metricas[5].detalle).toBe("Sin dato reciente");
  });
});
