// Datos de ejemplo de la pantalla de inicio (los mismos del diseño en Figma).
import type { ResumenInicio } from "@/modulos/inicio/tipos";

export const resumenInicioMock: ResumenInicio = {
  usuario: {
    nombre: "Carlos",
    nombreCompleto: "Carlos Perdomo",
    iniciales: "CP",
    rol: "Agricultor",
    municipio: "Neiva",
  },
  fecha: {
    larga: "Sábado 14 de noviembre de 2026",
    media: "Sáb. 14 nov. 2026",
    corta: "Sáb. 14 nov.",
  },
  finca: { nombre: "Finca La Esperanza", municipio: "Neiva", departamento: "Huila" },
  lote: {
    id: "el-mirador",
    nombre: "El Mirador",
    area: "4,2 ha",
    coordenadas: "2,9273° N, 75,2819° O",
    altitud: "442 m s. n. m.",
  },
  clima: {
    temperatura: "29°",
    condicion: "Parcialmente nublado",
    detalle: "Sensación térmica 32 °C · Máx. 33° / Mín. 22°",
    lluviaHoy: "Probabilidad de lluvia hoy: 70 % (tarde y noche)",
    fuentes: "Fuentes: NASA POWER · GPM IMERG · SMAP — Actualizado hoy, 9:40 a. m.",
  },
  alertaDestacada: {
    nivel: "naranja",
    titulo: "Lluvias fuertes y posibles crecientes súbitas",
    descripcion:
      "Cuenca alta del río Magdalena y afluentes en el Huila. Se esperan más de 40 mm de lluvia en las próximas 24 horas.",
    vigencia: "Vigente hasta: dom. 15 nov., 6:00 p. m.",
  },
  metricas: [
    { icono: "thermo", nombre: "Temperatura del aire", valor: "29", unidad: "°C", detalle: "+1,5 °C vs. promedio", fuente: "NASA POWER" },
    { icono: "rain", nombre: "Precipitación (24 h)", valor: "12,4", unidad: "mm", detalle: "Acumulado 7 días: 86 mm", fuente: "GPM IMERG" },
    { icono: "drop", nombre: "Humedad relativa", valor: "68", unidad: "%", detalle: "Rango óptimo: 60–80 %", fuente: "NASA POWER" },
    { icono: "wind", nombre: "Viento", valor: "11", unidad: "km/h", detalle: "Dirección NE · ráfagas 24", fuente: "NASA POWER" },
    { icono: "sun", nombre: "Radiación solar", valor: "5,6", unidad: "kWh/m²", detalle: "Por día · índice UV 9", fuente: "NASA POWER" },
    { icono: "layers", nombre: "Humedad del suelo", valor: "0,27", unidad: "m³/m³", detalle: "Adecuada para siembra", fuente: "SMAP" },
  ],
  pronostico: [
    { dia: "Hoy", icono: "rain", temperaturas: "33° / 22°", lluviaMm: 12, lluviaFuerte: false },
    { dia: "Dom", icono: "rain", temperaturas: "30° / 21°", lluviaMm: 38, lluviaFuerte: true },
    { dia: "Lun", icono: "rain", temperaturas: "29° / 21°", lluviaMm: 24, lluviaFuerte: false },
    { dia: "Mar", icono: "cloudsun", temperaturas: "31° / 22°", lluviaMm: 6, lluviaFuerte: false },
    { dia: "Mié", icono: "sun", temperaturas: "33° / 23°", lluviaMm: 0, lluviaFuerte: false },
    { dia: "Jue", icono: "sun", temperaturas: "34° / 23°", lluviaMm: 0, lluviaFuerte: false },
    { dia: "Vie", icono: "cloudsun", temperaturas: "32° / 22°", lluviaMm: 9, lluviaFuerte: false },
  ],
  alertas: [
    { nivel: "naranja", titulo: "Crecientes súbitas", detalle: "Río Magdalena y afluentes · hasta dom. 6:00 p. m." },
    { nivel: "amarilla", titulo: "Deslizamientos de tierra", detalle: "Zonas de ladera del norte del Huila · 48 horas" },
    { nivel: "amarilla", titulo: "Tormentas eléctricas", detalle: "Neiva y municipios vecinos · esta tarde" },
  ],
  alertasPendientes: 3,
  recomendacion: {
    titulo: "Aplaza la siembra de maíz hasta el miércoles",
    descripcion:
      "Con las lluvias fuertes del fin de semana el suelo quedará saturado. Desde el miércoles se espera tiempo seco y buena radiación.",
    rotacion: "Maíz → Fríjol → Arroz",
  },
};
