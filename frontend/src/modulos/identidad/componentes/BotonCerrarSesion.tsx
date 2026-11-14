"use client";

// Cierra la sesión (revoca el refresh token en el backend) y vuelve a la bienvenida.
import { useRouter } from "next/navigation";
import { useState } from "react";
import { cerrarSesion } from "../api";

interface Props {
  /**
   * "lateral": barra lateral verde. Solo icono en tablet (72 px) e icono + texto en escritorio ancho.
   * "movil": barra superior blanca del celular, solo icono (igual al botón de notificaciones).
   */
  variante: "lateral" | "movil";
}

function IconoSalir({ className }: { className: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 4h3a2 2 0 012 2v12a2 2 0 01-2 2h-3M10 16l-4-4 4-4M6 12h10" />
    </svg>
  );
}

export function BotonCerrarSesion({ variante }: Props) {
  const router = useRouter();
  const [saliendo, setSaliendo] = useState(false);

  async function salir() {
    setSaliendo(true);
    await cerrarSesion().catch(() => undefined);
    router.replace("/");
  }

  const texto = saliendo ? "Saliendo…" : "Cerrar sesión";

  if (variante === "movil") {
    return (
      <button
        type="button"
        onClick={() => void salir()}
        disabled={saliendo}
        aria-label={texto}
        title={texto}
        className="rounded-xl border border-borde bg-white p-2 text-tinta-suave disabled:opacity-60"
      >
        <IconoSalir className="size-[18px]" />
      </button>
    );
  }

  return (
    <button
      type="button"
      onClick={() => void salir()}
      disabled={saliendo}
      aria-label={texto}
      title={texto}
      className="flex size-11 items-center justify-center gap-3 rounded-[10px] text-verde-niebla hover:bg-white/8 hover:text-white disabled:opacity-60 2xl:h-auto 2xl:w-full 2xl:justify-start 2xl:px-[14px] 2xl:py-3"
    >
      <IconoSalir className="size-5 shrink-0" />
      <span className="hidden text-[15px] font-medium 2xl:block">{texto}</span>
    </button>
  );
}
