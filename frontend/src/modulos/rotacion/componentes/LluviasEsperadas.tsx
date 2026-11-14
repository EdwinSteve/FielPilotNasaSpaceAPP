// "Lluvias esperadas": mes a mes, la lluvia en palabras con gotas (o sol) y una nota corta.
import { Icono } from "@/compartido/ui/Icono";
import type { PlanRotacion } from "../tipos";
import { lluviaEnPalabras } from "./lluvias";

export function LluviasEsperadas({ lluvias }: { lluvias: PlanRotacion["lluvias"] }) {
  if (lluvias.meses.length === 0) return null;

  return (
    <section
      aria-labelledby="titulo-lluvias"
      className="flex flex-col gap-4 rounded-2xl border border-borde bg-white p-4 md:p-5 2xl:col-start-1 2xl:p-6"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="flex flex-col gap-1">
          <h2 id="titulo-lluvias" className="text-[16px] font-semibold md:text-[18px]">
            Lluvias esperadas
          </h2>
          <p className="text-[13px] text-tinta-suave">Para planear la siembra, el abono y la cosecha.</p>
        </div>
        <span className="shrink-0 rounded-md bg-fondo px-2 py-[3px] text-[10px] font-semibold text-tinta-suave">
          {lluvias.fuente}
        </span>
      </div>

      <ol className="grid gap-2 2xl:grid-cols-2">
        {lluvias.meses.map((m, i) => {
          const { texto, gotas, fondo } = lluviaEnPalabras(m);
          return (
            <li key={m.mes} className={`flex items-center gap-3 rounded-xl p-3 ${fondo}`}>
              <div className="flex w-[92px] shrink-0 flex-col gap-[2px] md:w-[110px]">
                <span className="text-[15px] font-bold">{m.mes}</span>
                <span className="flex flex-col text-[12px] font-medium text-tinta-suave">
                  {i === 0 && <span>Este mes</span>}
                  <span className="whitespace-nowrap">{m.lluviaMm} mm</span>
                </span>
              </div>
              <div className="flex min-w-0 flex-1 flex-col items-end gap-[3px] text-right">
                <span className="flex items-center gap-[6px] text-[14px] font-semibold">
                  <span aria-hidden className="flex">
                    {gotas === 0 ? (
                      <Icono nombre="sun" tam={18} />
                    ) : (
                      Array.from({ length: gotas }, (_, g) => <Icono key={g} nombre="gota-llena" tam={15} />)
                    )}
                  </span>
                  {texto}
                </span>
                {m.nota && <span className="text-[12px] leading-[17px] text-tinta-suave">{m.nota}</span>}
              </div>
            </li>
          );
        })}
      </ol>
    </section>
  );
}
