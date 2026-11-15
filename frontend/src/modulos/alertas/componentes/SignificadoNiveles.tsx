// "Qué significa cada nivel del IDEAM": guía fija con los cuatro niveles de la alerta hidrológica.
import type { NivelRios } from "../tipos";
import { NIVELES_RIOS } from "./niveles";
import { CLASE_TARJETA, Chip, EncabezadoTarjeta } from "./piezas";

const EN_ORDEN: NivelRios[] = ["NORMAL", "AMARILLA", "NARANJA", "ROJA"];

export function SignificadoNiveles() {
  return (
    <section className={`${CLASE_TARJETA} gap-[10px] 2xl:gap-3`}>
      <EncabezadoTarjeta titulo="Qué significa cada nivel del IDEAM" />
      <ul className="flex flex-col gap-[10px] 2xl:gap-3">
        {EN_ORDEN.map((id) => {
          const n = NIVELES_RIOS[id];
          return (
            <li
              key={id}
              className="flex flex-col items-start gap-2 rounded-xl bg-fondo p-3 md:flex-row md:items-center md:gap-4 md:p-[14px]"
            >
              <span className="md:w-[196px] md:shrink-0">
                <Chip tono={n.tono}>{n.etiqueta}</Chip>
              </span>
              <p className="text-[13px] leading-[1.45] md:flex-1 2xl:text-[14px]">
                {n.titulo} {n.accion}
              </p>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
