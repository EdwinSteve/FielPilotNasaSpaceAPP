// Marco común de las pantallas de ingreso y registro, con los estándares del diseño de Figma.
// Celular: el formulario ocupa todo el ancho sobre el fondo. Tablet y escritorio: tarjeta blanca centrada
// (radio de 16 px, borde de 1 px); el registro usa una tarjeta más ancha para los campos en dos columnas.
import Link from "next/link";
import type { ReactNode } from "react";
import { Marca } from "@/compartido/ui";

interface Props {
  titulo: string;
  subtitulo: string;
  children: ReactNode;
  pie?: ReactNode;
  /** "amplio" para formularios largos que en escritorio van en dos columnas. */
  ancho?: "angosto" | "amplio";
}

const ANCHOS = {
  angosto: "sm:max-w-md",
  amplio: "sm:max-w-xl lg:max-w-3xl",
};

export function MarcoAutenticacion({ titulo, subtitulo, children, pie, ancho = "angosto" }: Props) {
  return (
    <main className="flex min-h-dvh flex-col bg-fondo text-tinta sm:items-center sm:justify-center sm:px-6 sm:py-10">
      <div
        className={`flex w-full flex-col gap-6 px-4 py-6 sm:rounded-2xl sm:border sm:border-borde sm:bg-white sm:p-8 lg:p-10 ${ANCHOS[ancho]}`}
      >
        <div className="flex items-center justify-between">
          <Link href="/" className="inline-flex min-h-11 items-center gap-1 text-[14px] font-semibold text-verde-700 hover:underline">
            <span aria-hidden="true">←</span> Volver
          </Link>
          <Marca />
        </div>

        <header className="flex flex-col gap-1">
          <h1 className="text-[22px] font-bold md:text-[24px] 2xl:text-[30px]">{titulo}</h1>
          <p className="text-[14px] leading-relaxed text-tinta-suave md:text-[15px]">{subtitulo}</p>
        </header>

        {children}

        {pie && <p className="text-center text-[14px] text-tinta-suave">{pie}</p>}
      </div>
    </main>
  );
}
