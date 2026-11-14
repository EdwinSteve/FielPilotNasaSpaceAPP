"use client";

// Texto completo de la política de datos que el productor acepta al registrarse.
// En una tarjeta blanca como el resto de la app (estándares del diseño de Figma).
import Link from "next/link";
import { Marca, TarjetaError } from "@/compartido/ui";
import { usePoliticaVigente } from "../hooks/usePoliticaVigente";

export function TextoPolitica() {
  const { politica, cargando, error, recargar } = usePoliticaVigente();

  return (
    <main className="min-h-dvh bg-fondo text-tinta sm:px-6 sm:py-10">
      <article className="mx-auto flex max-w-2xl flex-col gap-5 px-4 py-6 sm:rounded-2xl sm:border sm:border-borde sm:bg-white sm:p-8 lg:p-10">
        <div className="flex items-center justify-between">
          <Link href="/registro" className="inline-flex min-h-11 items-center gap-1 text-[14px] font-semibold text-verde-700 hover:underline">
            <span aria-hidden="true">←</span> Volver al registro
          </Link>
          <Marca />
        </div>
        <header className="flex flex-col gap-1">
          <h1 className="text-[22px] font-bold md:text-[24px] 2xl:text-[30px]">Política de tratamiento de datos</h1>
          {politica && <p className="text-[13px] text-tinta-suave">Versión {politica.version}</p>}
        </header>
        {error && <TarjetaError mensaje={error} alReintentar={recargar} />}
        {cargando && (
          <p role="status" className="text-[15px] text-tinta-suave">
            Cargando...
          </p>
        )}
        {politica && <div className="whitespace-pre-line text-[15px] leading-relaxed text-tinta">{politica.contenido}</div>}
      </article>
    </main>
  );
}
