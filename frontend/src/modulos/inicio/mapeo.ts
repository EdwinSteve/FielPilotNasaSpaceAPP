// Convierte la respuesta del backend (números) en lo que pinta la pantalla (textos con unidades, en español).
// Las fechas y horas se muestran siempre en hora de Colombia, esté donde esté el navegador.
import type { AlertaResumen, AvisoApi, DatosInicio, Metrica, NivelAlerta, ResumenClimaApi } from "./tipos";

const ZONA = "America/Bogota";
const SIN_DATO = "—";

const entero = new Intl.NumberFormat("es-CO", { maximumFractionDigits: 0 });
const decimal = new Intl.NumberFormat("es-CO", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const coordenada = new Intl.NumberFormat("es-CO", { minimumFractionDigits: 4, maximumFractionDigits: 4 });

const NIVELES: Record<AvisoApi["nivel"], NivelAlerta> = { AMARILLA: "amarilla", NARANJA: "naranja", ROJA: "roja" };

function formato(fecha: Date, opciones: Intl.DateTimeFormatOptions): string {
  return new Intl.DateTimeFormat("es-CO", { timeZone: ZONA, ...opciones }).format(fecha);
}

/** Una parte de la fecha ("dom", "nov", "15"), sin el punto que algunos navegadores ponen a las abreviaturas. */
function parte(fecha: Date, opciones: Intl.DateTimeFormatOptions): string {
  return formato(fecha, opciones).replace(/\.$/, "");
}

function mayuscula(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

/** "2026-11-15" es un día del calendario de Colombia: se ancla al mediodía para que ninguna zona lo corra. */
function dia(fechaIso: string): Date {
  return new Date(`${fechaIso}T12:00:00-05:00`);
}

/** "2026-11-12" → "12 nov." */
function diaCorto(fechaIso: string): string {
  const fecha = dia(fechaIso);
  return `${parte(fecha, { day: "numeric" })} ${parte(fecha, { month: "short" })}.`;
}

function fechas(ahora: Date): DatosInicio["fecha"] {
  const numero = parte(ahora, { day: "numeric" });
  const anio = parte(ahora, { year: "numeric" });
  const semanaCorta = `${mayuscula(parte(ahora, { weekday: "short" }))}.`;
  const mesCorto = `${parte(ahora, { month: "short" })}.`;
  return {
    larga: `${mayuscula(parte(ahora, { weekday: "long" }))} ${numero} de ${parte(ahora, { month: "long" })} de ${anio}`,
    media: `${semanaCorta} ${numero} ${mesCorto} ${anio}`,
    corta: `${semanaCorta} ${numero} ${mesCorto}`,
  };
}

function saludo(ahora: Date): string {
  const hora = Number(parte(ahora, { hour: "numeric", hourCycle: "h23" }));
  if (hora < 12) return "Buenos días";
  return hora < 18 ? "Buenas tardes" : "Buenas noches";
}

/** "2,9273° N, 75,2819° O" */
function coordenadas(lat: number, lon: number): string {
  return `${coordenada.format(Math.abs(lat))}° ${lat >= 0 ? "N" : "S"}, ${coordenada.format(Math.abs(lon))}° ${lon >= 0 ? "E" : "O"}`;
}

function aAlerta(aviso: AvisoApi): AlertaResumen {
  return {
    nivel: NIVELES[aviso.nivel],
    titulo: aviso.titulo,
    detalle: aviso.zona ? `${aviso.zona} · ${aviso.mensaje}` : aviso.mensaje,
    fuente: aviso.fuente,
    oficial: aviso.oficial,
  };
}

function metricas(api: ResumenClimaApi): Metrica[] {
  const { actual, satelite } = api;
  const hoy = api.pronostico[0];
  const fuenteTiempo = api.fuentes[0] ?? "Open-Meteo";
  const uv = hoy?.indice_uv != null ? `Índice UV máximo hoy: ${entero.format(hoy.indice_uv)}` : "";

  const viento = [
    actual.viento_rumbo ? `Viene del ${actual.viento_rumbo}` : "",
    actual.rafagas_kmh != null ? `ráfagas ${entero.format(actual.rafagas_kmh)} km/h` : "",
  ].filter(Boolean);

  return [
    {
      icono: "thermo",
      nombre: "Temperatura del aire",
      valor: entero.format(actual.temperatura),
      unidad: "°C",
      detalle: actual.sensacion != null ? `Sensación térmica ${entero.format(actual.sensacion)} °C` : "",
      fuente: fuenteTiempo,
    },
    {
      icono: "rain",
      nombre: "Precipitación (24 h)",
      valor: decimal.format(actual.lluvia_dia_mm),
      unidad: "mm",
      detalle: `Acumulado 7 días: ${entero.format(actual.lluvia_semana_mm)} mm`,
      fuente: fuenteTiempo,
    },
    {
      icono: "drop",
      nombre: "Humedad relativa",
      valor: actual.humedad != null ? entero.format(actual.humedad) : SIN_DATO,
      unidad: "%",
      detalle: hoy?.prob_lluvia != null ? `Probabilidad de lluvia hoy: ${hoy.prob_lluvia} %` : "",
      fuente: fuenteTiempo,
    },
    {
      icono: "wind",
      nombre: "Viento",
      valor: actual.viento_kmh != null ? entero.format(actual.viento_kmh) : SIN_DATO,
      unidad: "km/h",
      detalle: viento.join(" · "),
      fuente: fuenteTiempo,
    },
    {
      icono: "sun",
      nombre: "Radiación solar",
      valor: satelite?.radiacion_kwh != null ? decimal.format(satelite.radiacion_kwh) : SIN_DATO,
      unidad: "kWh/m²",
      detalle:
        satelite?.radiacion_kwh != null && satelite.radiacion_fecha
          ? `Total del ${diaCorto(satelite.radiacion_fecha)}`
          : uv || "Sin dato reciente",
      fuente: "NASA POWER",
    },
    {
      icono: "layers",
      nombre: "Humedad del suelo",
      valor: satelite?.humedad_suelo_pct != null ? entero.format(satelite.humedad_suelo_pct) : SIN_DATO,
      unidad: "%",
      detalle:
        satelite?.humedad_suelo_pct != null && satelite.humedad_suelo_fecha
          ? `Zona de raíces · ${diaCorto(satelite.humedad_suelo_fecha)}`
          : "Sin dato reciente",
      fuente: "NASA POWER",
    },
  ];
}

export function aDatosInicio(api: ResumenClimaApi, ahora: Date = new Date()): DatosInicio {
  const { actual, ubicacion } = api;
  const hoy = api.pronostico[0];
  const alertas = api.avisos.map(aAlerta);
  const actualizado = formato(new Date(api.actualizado), { hour: "numeric", minute: "2-digit", hour12: true });

  const detalle = [
    actual.sensacion != null ? `Sensación térmica ${entero.format(actual.sensacion)} °C` : "",
    hoy ? `Máx. ${entero.format(hoy.temp_max)}° / Mín. ${entero.format(hoy.temp_min)}°` : "",
  ].filter(Boolean);

  const lluviaHoy = [
    hoy?.prob_lluvia != null ? `Probabilidad de lluvia hoy: ${hoy.prob_lluvia} %` : "",
    hoy ? `se esperan ${decimal.format(hoy.lluvia_mm)} mm` : "",
  ].filter(Boolean);

  return {
    saludo: saludo(ahora),
    fecha: fechas(ahora),
    ubicacion: {
      nombre: ubicacion.municipio ?? "Su ubicación",
      departamento: ubicacion.departamento ?? "",
      coordenadas: coordenadas(ubicacion.lat, ubicacion.lon),
      altitud: ubicacion.altitud_m != null ? `${entero.format(ubicacion.altitud_m)} m s. n. m.` : "",
    },
    clima: {
      temperatura: `${entero.format(actual.temperatura)}°`,
      condicion: actual.descripcion,
      detalle: detalle.join(" · "),
      lluviaHoy: mayuscula(lluviaHoy.join(" · ")),
      fuentes: `Fuentes: ${api.fuentes.join(" · ")} — Actualizado hoy, ${actualizado}`,
    },
    alertaDestacada: alertas[0] ?? null,
    sinAlertas: api.fuentes.includes("IDEAM")
      ? "El IDEAM no reporta alertas en los ríos y quebradas de su zona y no se espera lluvia fuerte en los próximos 3 días."
      : "No se espera lluvia fuerte en los próximos 3 días. En este momento no pudimos consultar las alertas del IDEAM.",
    metricas: metricas(api),
    pronostico: api.pronostico.map((d, i) => ({
      dia: i === 0 ? "Hoy" : mayuscula(parte(dia(d.fecha), { weekday: "short" })),
      icono: d.icono,
      temperaturas: `${entero.format(d.temp_max)}° / ${entero.format(d.temp_min)}°`,
      lluviaMm: Math.round(d.lluvia_mm),
      lluviaFuerte: d.lluvia_fuerte,
    })),
    alertas,
    alertasPendientes: alertas.length,
    recomendacion: null,
  };
}
