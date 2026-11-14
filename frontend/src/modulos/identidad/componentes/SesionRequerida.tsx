"use client";

// Envuelve una pantalla que exige sesión: mientras carga el perfil muestra un aviso, si falla la red
// permite reintentar, y cuando hay usuario renderiza la pantalla con él.
import type { ReactNode } from "react";
import { TarjetaError } from "@/compartido/ui";
import { useSesionRequerida, type UsuarioSesion } from "../hooks/useSesionRequerida";

export function SesionRequerida({ children }: { children: (usuario: UsuarioSesion) => ReactNode }) {
  const { usuario, error, reintentar } = useSesionRequerida();

  if (usuario) return <>{children(usuario)}</>;

  return (
    <main className="flex min-h-dvh items-center justify-center bg-fondo p-4">
      <div className="w-full max-w-md">
        {error ? (
          <TarjetaError mensaje={error.mensaje} codigo={error.codigo} alReintentar={reintentar} />
        ) : (
          <p role="status" className="text-center text-[15px] text-tinta-suave">
            Cargando su cuenta…
          </p>
        )}
      </div>
    </main>
  );
}
