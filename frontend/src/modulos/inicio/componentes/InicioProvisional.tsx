"use client";

// Inicio provisional: pide el perfil a una ruta protegida (GET /usuarios/me) para confirmar que la sesión
// y el JWT funcionan, mientras se construyen el resumen climatológico y la recomendación.
// Sin sesión, o si la sesión ya no se puede renovar, manda al ingreso.
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { aErrorApi, type ErrorApi } from "@/compartido/api/tipos";
import { TarjetaError } from "@/compartido/ui";
import { cerrarSesion, haySesion, obtenerMiPerfil, type Usuario } from "@/modulos/identidad";

const SESION_TERMINADA = new Set(["AUTH_NO_AUTENTICADO", "AUTH_TOKEN_INVALIDO", "AUTH_REFRESH_INVALIDO", "HTTP_401"]);

export function InicioProvisional() {
  const router = useRouter();
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [error, setError] = useState<ErrorApi | null>(null);
  const [intento, setIntento] = useState(0);
  const [saliendo, setSaliendo] = useState(false);

  useEffect(() => {
    if (!haySesion()) {
      router.replace("/ingresar");
      return;
    }
    let cancelado = false;
    setError(null);
    obtenerMiPerfil()
      .then((u) => !cancelado && setUsuario(u))
      .catch((err) => {
        if (cancelado) return;
        const e = aErrorApi(err);
        if (SESION_TERMINADA.has(e.codigo)) router.replace("/ingresar");
        else setError(e);
      });
    return () => {
      cancelado = true;
    };
  }, [router, intento]);

  async function salir() {
    setSaliendo(true);
    await cerrarSesion().catch(() => undefined);
    router.replace("/");
  }

  const primerNombre = usuario?.nombre_completo.split(/\s+/)[0];

  return (
    <main className="min-h-dvh bg-crema text-tinta">
      <div className="mx-auto flex max-w-md flex-col gap-6 px-4 py-8 sm:max-w-xl sm:px-6">
        <div className="flex items-center justify-between">
          <span className="font-titulo text-lg font-black tracking-tight text-campo">SURCO</span>
          <button
            type="button"
            onClick={() => void salir()}
            disabled={saliendo}
            className="min-h-11 px-3 font-bold text-campo underline disabled:opacity-60"
          >
            {saliendo ? "Cerrando…" : "Cerrar sesión"}
          </button>
        </div>

        {error && <TarjetaError mensaje={error.mensaje} codigo={error.codigo} alReintentar={() => setIntento((n) => n + 1)} />}

        {!usuario && !error && (
          <p role="status" className="text-lg text-tinta/70">
            Cargando su cuenta…
          </p>
        )}

        {usuario && (
          <>
            <h1 className="font-titulo text-3xl font-black text-campo">¡Hola, {primerNombre}!</h1>
            <div className="rounded-2xl border-2 border-arena bg-white p-5">
              <p className="text-lg font-extrabold">Próximo paso: registrar su finca y su lote</p>
              <p className="mt-1 text-tinta/80">
                Con la ubicación de su lote armaremos el resumen del clima de su terreno y le recomendaremos qué sembrar.
                Esta pantalla está en desarrollo.
              </p>
            </div>
          </>
        )}
      </div>
    </main>
  );
}
