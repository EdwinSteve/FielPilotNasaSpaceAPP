// Marco común de las pantallas de ingreso y registro.
// Celular: el formulario ocupa todo el ancho. Tablet y escritorio: tarjeta blanca centrada;
// el registro usa una tarjeta más ancha para poner los campos en dos columnas.
import Link from "next/link";
import type { ReactNode } from "react";

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
    <main className="flex min-h-dvh flex-col bg-crema text-tinta sm:items-center sm:justify-center sm:px-6 sm:py-10">
      <div
        className={`flex w-full flex-col gap-6 px-4 py-6 sm:rounded-3xl sm:border-2 sm:border-arena sm:bg-white sm:p-8 sm:shadow-sm lg:p-10 ${ANCHOS[ancho]}`}
      >
        <div className="flex items-center justify-between">
          <Link href="/" className="inline-flex min-h-11 items-center font-bold text-campo hover:underline">
            ← Volver
          </Link>
          <span className="font-titulo text-lg font-black tracking-tight text-campo">SURCO</span>
        </div>

        <header>
          <h1 className="font-titulo text-3xl font-black text-campo lg:text-4xl">{titulo}</h1>
          <p className="mt-2 text-lg text-tinta/80">{subtitulo}</p>
        </header>

        {children}

        {pie && <p className="text-center text-base">{pie}</p>}
      </div>
    </main>
  );
}
