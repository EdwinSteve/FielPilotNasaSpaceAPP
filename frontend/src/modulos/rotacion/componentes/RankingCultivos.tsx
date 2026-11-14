// "Qué tanto le conviene cada cultivo a su lote": barra de 0 a 1; los no aptos al final y en gris.
import { formatearPuntaje } from "@/compartido/lib/formato";
import { Icono } from "@/compartido/ui/Icono";
import type { CultivoRanking } from "../tipos";

/** Los aptos primero, del que más le conviene al que menos; los no aptos al final. */
export function ordenarRanking(ranking: CultivoRanking[]): CultivoRanking[] {
  return [...ranking].sort((a, b) => Number(b.apto) - Number(a.apto) || b.puntaje - a.puntaje);
}

function Barra({ puntaje, apto }: { puntaje: number; apto: boolean }) {
  const ancho = `${Math.round(Math.max(0, Math.min(1, puntaje)) * 100)}%`;
  return (
    <span aria-hidden className="block h-[10px] w-full overflow-hidden rounded-full bg-borde">
      <span className={`block h-full rounded-full ${apto ? "bg-verde-700" : "bg-apagado"}`} style={{ width: ancho }} />
    </span>
  );
}

export function RankingCultivos({ ranking }: { ranking: CultivoRanking[] }) {
  return (
    <section
      aria-labelledby="titulo-ranking"
      className="flex flex-col gap-4 rounded-2xl border border-borde bg-white p-4 md:p-5 2xl:col-start-2 2xl:row-span-2 2xl:row-start-1 2xl:p-6"
    >
      <div className="flex flex-col gap-1">
        <h2 id="titulo-ranking" className="text-[16px] font-semibold md:text-[18px]">
          Qué tanto le conviene cada cultivo a su lote
        </h2>
        <p className="text-[13px] text-tinta-suave">De 0 a 1: entre más larga la barra, mejor le va en su lote.</p>
      </div>

      <ol className="flex flex-col gap-1">
        {ordenarRanking(ranking).map((c) => {
          const valor = formatearPuntaje(c.puntaje);
          if (!c.apto) {
            return (
              <li key={c.id} className="flex flex-col gap-2 rounded-xl bg-fondo px-3 py-[10px]">
                <span className="flex flex-wrap items-center justify-between gap-2">
                  <span className="text-[15px] font-semibold text-tinta-suave">{c.nombre}</span>
                  <span className="inline-flex items-center gap-[5px] rounded-full bg-roja-fondo px-[10px] py-1 text-[12px] font-semibold text-roja-texto">
                    <Icono nombre="alert-roja" tam={14} />
                    No recomendado aquí
                  </span>
                </span>
                <Barra puntaje={c.puntaje} apto={false} />
                {c.razonNoApto && <span className="text-[13px] text-tinta-suave">{c.razonNoApto}</span>}
              </li>
            );
          }
          return (
            <li key={c.id} className="flex flex-col gap-2 rounded-xl px-3 py-[10px]">
              <span className="flex items-center justify-between gap-2">
                <span className="text-[15px] font-semibold">{c.nombre}</span>
                <span className="flex items-center gap-1 text-[15px] font-bold text-verde-700">
                  {valor}
                  <span className="sr-only"> de 1</span>
                  <Icono nombre="chevron-derecha" tam={16} />
                </span>
              </span>
              <Barra puntaje={c.puntaje} apto />
            </li>
          );
        })}
      </ol>
    </section>
  );
}
