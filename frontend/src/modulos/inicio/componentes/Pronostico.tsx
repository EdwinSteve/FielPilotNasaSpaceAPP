import { Icono } from "@/compartido/ui/Icono";
import type { DiaPronostico } from "../tipos";

function colorBarra(dia: DiaPronostico) {
  if (dia.lluviaFuerte) return "bg-naranja";
  return dia.lluviaMm === 0 ? "bg-lluvia/25" : "bg-lluvia";
}

export function Pronostico({ dias }: { dias: DiaPronostico[] }) {
  const maximo = Math.max(...dias.map((d) => d.lluviaMm), 1);

  return (
    <section className="flex flex-col gap-3 rounded-2xl border border-borde bg-white p-4 md:col-span-2 md:gap-4 md:p-5 2xl:col-span-1 2xl:gap-5 2xl:p-6">
      <div className="flex items-center justify-between">
        <h2 className="text-[16px] font-semibold 2xl:text-[18px]">
          <span className="md:hidden">Pronóstico 7 días</span>
          <span className="hidden md:inline">Pronóstico para los próximos 7 días</span>
        </h2>
        <p className="text-[12px] text-tinta-suave 2xl:text-[13px]">
          <span className="md:hidden">Lluvia (mm)</span>
          <span className="hidden md:inline">Lluvia esperada (mm)</span>
        </p>
      </div>

      {/* Móvil: una fila por día con barra horizontal */}
      <ul className="flex flex-col gap-3 md:hidden">
        {dias.map((d, i) => (
          <li
            key={d.dia}
            className={`flex h-[38px] items-center gap-[10px] rounded-[10px] px-[10px] ${i === 0 ? "bg-verde-100" : ""}`}
          >
            <span className="w-[30px] text-[14px] font-semibold">{d.dia}</span>
            <Icono nombre={`${d.icono}-28`} tam={28} className="size-5" />
            <span className="h-2 flex-1 rounded-full bg-borde">
              {d.lluviaMm > 0 && (
                <span
                  className={`block h-full rounded-full ${colorBarra(d)}`}
                  style={{ width: `${(d.lluviaMm / maximo) * 92.7}%` }}
                />
              )}
            </span>
            <span
              className={`w-[44px] text-right text-[13px] font-semibold ${d.lluviaFuerte ? "text-naranja-texto" : "text-tinta-suave"}`}
            >
              {d.lluviaMm} mm
            </span>
            <span className="w-[58px] text-right text-[13px] font-medium">{d.temperaturas}</span>
          </li>
        ))}
      </ul>

      {/* Tablet y escritorio: una columna por día con barra vertical */}
      <ul className="hidden flex-1 gap-2 md:flex 2xl:gap-3">
        {dias.map((d, i) => (
          <li
            key={d.dia}
            className={`flex min-h-[194px] min-w-0 flex-1 flex-col items-center justify-between rounded-xl px-2 py-[14px] 2xl:min-h-[250px] 2xl:py-4 ${i === 0 ? "bg-verde-100" : "bg-fondo"}`}
          >
            <span className="text-[13px] font-semibold 2xl:text-[14px]">{d.dia}</span>
            <Icono nombre={`${d.icono}-28`} tam={28} className="size-6 2xl:size-7" />
            <span className="text-[12px] font-medium whitespace-nowrap 2xl:text-[14px]">
              {d.temperaturas}
            </span>
            <span className="flex h-14 w-5 items-end justify-center 2xl:h-[72px] 2xl:w-6">
              <span
                className={`w-4 rounded-md 2xl:w-5 ${colorBarra(d)}`}
                style={{ height: `max(4px, ${(d.lluviaMm / maximo) * 94.5}%)` }}
              />
            </span>
            <span
              className={`text-[12px] font-semibold 2xl:text-[13px] ${d.lluviaFuerte ? "text-naranja-texto" : "text-tinta-suave"}`}
            >
              {d.lluviaMm} mm
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}
