// "Tus terrenos": el nivel de ríos y de sequía de cada lote. El lote elegido va resaltado.
import Link from "next/link";
import { Icono } from "@/compartido/ui/Icono";
import type { AlertasLote, LoteUbicado } from "../tipos";
import { NIVELES_RIOS, NIVELES_SEQUIA, type Tono } from "./niveles";
import { CLASE_TARJETA, Chip, EncabezadoTarjeta } from "./piezas";

function resumen({ rios, sequia }: AlertasLote) {
  const chipRios: [Tono, string] =
    rios.estado === "ok"
      ? [NIVELES_RIOS[rios.datos.nivel].tono, NIVELES_RIOS[rios.datos.nivel].corto]
      : ["gris", rios.estado === "cargando" ? "…" : "Sin datos"];
  const chipSequia: [Tono, string] =
    sequia.estado === "ok"
      ? [NIVELES_SEQUIA[sequia.datos.nivel].tono, NIVELES_SEQUIA[sequia.datos.nivel].corto]
      : ["gris", sequia.estado === "cargando" ? "…" : "Sin datos"];

  let zona = "Consultando…";
  if (rios.estado === "ok") zona = rios.datos.subzona ?? "Sin alerta en su subzona";
  else if (rios.estado === "error")
    zona = rios.fallo === "UBICACION_INVALIDA" ? "Ubicación fuera de Colombia" : "Sin datos del IDEAM";

  return { zona, chipRios, chipSequia };
}

export function TusTerrenos({
  lotes,
  actualId,
  alertasDe,
}: {
  lotes: LoteUbicado[];
  actualId: string;
  alertasDe: (id: string) => AlertasLote;
}) {
  return (
    <section className={`${CLASE_TARJETA} gap-[10px] 2xl:gap-3`}>
      <EncabezadoTarjeta titulo="Tus terrenos" />
      <ul className="flex flex-col gap-[10px] 2xl:gap-3">
        {lotes.map((lote) => {
          const { zona, chipRios, chipSequia } = resumen(alertasDe(lote.id));
          const elegido = lote.id === actualId;
          return (
            <li key={lote.id}>
              <Link
                href={`/alertas?lote=${lote.id}`}
                aria-current={elegido ? "true" : undefined}
                className={`flex flex-col gap-2 rounded-xl p-[14px] transition-colors ${
                  elegido ? "bg-verde-100" : "bg-fondo hover:bg-verde-100/60"
                }`}
              >
                <span className="flex items-center gap-2">
                  <Icono nombre="pin-verde-16" tam={16} />
                  <span className="flex min-w-0 flex-col gap-px">
                    <span className="text-[15px] font-semibold">{lote.nombre}</span>
                    <span className="text-[12px] text-tinta-suave">{zona}</span>
                  </span>
                </span>
                <span className="flex flex-wrap gap-[6px]">
                  <Chip tono={chipRios[0]} sobreBlanco={chipRios[0] === "verde"}>Ríos: {chipRios[1]}</Chip>
                  <Chip tono={chipSequia[0]} sobreBlanco={chipSequia[0] === "verde"}>Sequía: {chipSequia[1]}</Chip>
                </span>
              </Link>
            </li>
          );
        })}
      </ul>
      <p className="text-[12px] leading-[1.45] text-tinta-suave">
        Cada terreno se consulta por su ubicación (latitud y longitud).
      </p>
    </section>
  );
}
