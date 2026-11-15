// Datos de ejemplo del plan de siembra (los mismos del diseño en Figma: Rotación de cultivos).
// Ninguna ruta los usa: la pantalla de rotación pide el plan real al backend (rotacion/api.ts).
// Quedan para pruebas.
import type { LoteOpcion, PlanRotacion } from "@/modulos/rotacion/tipos";

export const lotesMock: LoteOpcion[] = [
  { id: "el-mirador", nombre: "El Mirador", area: "4,2 ha" },
  { id: "la-vega", nombre: "La Vega", area: "6,4 ha" },
  { id: "el-guadual", nombre: "El Guadual", area: "3,2 ha" },
];

export function planRotacionMock(lote: LoteOpcion): PlanRotacion {
  return {
    planId: "plan-de-ejemplo",
    seleccion: null,
    usuario: { nombreCompleto: "Carlos Perdomo", iniciales: "CP", rol: "Agricultor", municipio: "Neiva" },
    alertasPendientes: 3,
    finca: { nombre: "Finca La Esperanza", municipio: "Neiva" },
    lote,
    lotes: lotesMock,
    rotaciones: [
      {
        id: "maiz-soya-sorgo",
        grado: "A",
        secuencia: ["Maíz", "Soya", "Sorgo"],
        frecuenciaExito: 8,
        aguaAhorrada: "2.100 m³",
        aporteNitrogeno: "La soya le deja al sorgo unos 60 kg de nitrógeno por hectárea",
        temporadas: [],
      },
      {
        id: "arroz-soya-maiz",
        grado: "B",
        secuencia: ["Arroz", "Soya", "Maíz"],
        frecuenciaExito: 7,
        aguaAhorrada: "900 m³",
        aporteNitrogeno: "La soya le deja al maíz unos 60 kg de nitrógeno por hectárea",
        temporadas: [],
      },
      {
        id: "sorgo-mani-maiz",
        grado: "B",
        secuencia: ["Sorgo", "Maní", "Maíz"],
        frecuenciaExito: 7,
        aguaAhorrada: "1.500 m³",
        aporteNitrogeno: "El maní le deja al maíz unos 40 kg de nitrógeno por hectárea",
        temporadas: [],
      },
    ],
    ranking: [
      { id: "arroz", nombre: "Arroz", puntaje: 0.83, apto: true, razonNoApto: null },
      { id: "maiz", nombre: "Maíz", puntaje: 0.71, apto: true, razonNoApto: null },
      { id: "sorgo", nombre: "Sorgo", puntaje: 0.67, apto: true, razonNoApto: null },
      { id: "cafe", nombre: "Café", puntaje: 0.12, apto: false, razonNoApto: "Su zona es muy caliente para el café." },
      { id: "cacao", nombre: "Cacao", puntaje: 0.61, apto: true, razonNoApto: null },
      { id: "soya", nombre: "Soya", puntaje: 0.56, apto: true, razonNoApto: null },
      { id: "frijol", nombre: "Fríjol", puntaje: 0.46, apto: true, razonNoApto: null },
    ],
    lluvias: {
      fuente: "NASA POWER",
      meses: [
        { mes: "Noviembre", lluviaMm: 180, nivel: "lluvioso", nota: "Lluvias seguidas: revise que el agua no se encharque." },
        { mes: "Diciembre", lluviaMm: 110, nivel: "normal", nota: null },
        { mes: "Enero", lluviaMm: 90, nivel: "normal", nota: "Bajan las lluvias: buen momento para cosechar." },
        { mes: "Febrero", lluviaMm: 110, nivel: "normal", nota: "Empiezan a volver las lluvias." },
        { mes: "Marzo", lluviaMm: 160, nivel: "lluvioso", nota: "Buena época para sembrar maíz." },
        { mes: "Abril", lluviaMm: 190, nivel: "lluvioso", nota: "Las lluvias más fuertes del año." },
      ],
    },
  };
}
