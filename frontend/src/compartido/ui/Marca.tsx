// Marca de la app: el brote blanco sobre el cuadro verde y el nombre, como en el diseño de Figma.
// El nombre vive solo aquí: cambiarlo aquí lo cambia en toda la app.
import { Icono } from "./Icono";

export const NOMBRE_APP = "Field Shift";

interface Props {
  /** "normal": barras y encabezados. "grande": portada de la bienvenida. */
  tam?: "normal" | "grande";
  /** "oscuro" para fondos claros (texto tinta); "claro" para la barra lateral verde (texto blanco). */
  tono?: "oscuro" | "claro";
  /** Oculta el nombre (por ejemplo en la barra lateral compacta). */
  soloIcono?: boolean;
}

export function Marca({ tam = "normal", tono = "oscuro", soloIcono = false }: Props) {
  const grande = tam === "grande";
  return (
    <span className="inline-flex items-center gap-2">
      <span className={`rounded-[10px] bg-verde-500 ${grande ? "p-2" : "p-[6px]"}`}>
        <Icono nombre="sprout-blanco" className={grande ? "size-6" : "size-4"} />
      </span>
      {!soloIcono && (
        <span className={`font-bold ${grande ? "text-[20px]" : "text-[16px]"} ${tono === "claro" ? "text-white" : "text-tinta"}`}>
          {NOMBRE_APP}
        </span>
      )}
    </span>
  );
}
