"use client";

// Texto completo de la política de datos que el productor acepta al registrarse.
import Link from "next/link";
import { TarjetaError } from "@/compartido/ui";
import { usePoliticaVigente } from "../hooks/usePoliticaVigente";

export function TextoPolitica() {
  const { politica, cargando, error, recargar } = usePoliticaVigente();

  return (
    <main className="min-h-dvh bg-crema text-tinta">
      <article className="mx-auto flex max-w-xl flex-col gap-4 px-4 py-8">
        <Link href="/registro" className="inline-flex min-h-11 items-center self-start font-bold text-campo hover:underline">
          ← Volver al registro
        </Link>
        <h1 className="font-titulo text-3xl font-black text-campo">Política de tratamiento de datos</h1>
        {error && <TarjetaError mensaje={error} alReintentar={recargar} />}
        {cargando && (
          <p role="status" className="text-base text-tinta/70">
            Cargando...
          </p>
        )}
        {politica && (
          <>
            <p className="text-sm font-bold text-tinta/60">Versión {politica.version}</p>
            <div className="whitespace-pre-line text-base leading-relaxed">{politica.contenido}</div>
          </>
        )}
      </article>
    </main>
  );
}
