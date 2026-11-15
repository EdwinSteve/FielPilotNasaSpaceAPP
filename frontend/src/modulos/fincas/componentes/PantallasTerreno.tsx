"use client";

// Pantallas "Mi terreno": la lista de terrenos del productor sobre el mapa y el registro de uno nuevo.
// Exigen sesión (SesionRequerida manda a /ingresar si no la hay).
import dynamic from "next/dynamic";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useState, type ReactNode } from "react";
import { aErrorApi, type ErrorApi } from "@/compartido/api/tipos";
import { AvisoExito, CLASE_BOTON_PRINCIPAL, TarjetaError } from "@/compartido/ui";
import { BarraSuperiorMovil } from "@/compartido/ui/BarraSuperiorMovil";
import { NavegacionPrincipal } from "@/compartido/ui/NavegacionPrincipal";
import { BotonCerrarSesion, SesionRequerida, type UsuarioSesion } from "@/modulos/identidad";
import { eliminarTerreno, listarTerrenos } from "../api";
import { formatoArea } from "../geometria";
import type { Terreno } from "../tipos";
import { FormularioTerreno } from "./FormularioTerreno";

const MapaTerreno = dynamic(() => import("./MapaTerreno").then((m) => m.MapaTerreno), { ssr: false });

/** Lote de "Cultivos y rotación" mientras esa pantalla siga con datos de ejemplo. */
const LOTE_ROTACION = "el-mirador";

function Marco({ usuario, titulo, subtitulo, children }: { usuario: UsuarioSesion; titulo: string; subtitulo: string; children: ReactNode }) {
  return (
    <div className="min-h-screen md:flex">
      <NavegacionPrincipal
        activa="terreno"
        usuario={usuario}
        loteId={LOTE_ROTACION}
        alertasPendientes={0}
        accionUsuario={<BotonCerrarSesion variante="lateral" />}
      />
      <main className="flex min-w-0 flex-1 flex-col gap-[14px] p-4 pb-[82px] md:gap-[18px] md:p-6 2xl:gap-6 2xl:px-10 2xl:py-8">
        <header className="flex flex-col gap-[14px]">
          <BarraSuperiorMovil iniciales={usuario.iniciales} accionUsuario={<BotonCerrarSesion variante="movil" />} />
          <div className="flex flex-col gap-[3px]">
            <h1 className="text-[22px] font-bold md:text-[24px] 2xl:text-[30px]">{titulo}</h1>
            <p className="text-[13px] text-tinta-suave md:text-[14px] 2xl:text-[15px]">{subtitulo}</p>
          </div>
        </header>
        {children}
      </main>
    </div>
  );
}

function useTerrenos() {
  const [terrenos, setTerrenos] = useState<Terreno[] | null>(null);
  const [error, setError] = useState<ErrorApi | null>(null);
  const cargar = useCallback(() => {
    setError(null);
    listarTerrenos()
      .then(setTerrenos)
      .catch((e) => setError(aErrorApi(e)));
  }, []);
  useEffect(cargar, [cargar]);
  return { terrenos, error, recargar: cargar, setTerrenos };
}

function ListaTerrenos({ usuario }: { usuario: UsuarioSesion }) {
  const { terrenos, error, recargar, setTerrenos } = useTerrenos();
  const [borrando, setBorrando] = useState<string | null>(null);
  const [errorBorrar, setErrorBorrar] = useState<ErrorApi | null>(null);
  const nuevo = useSearchParams().get("nuevo");

  async function borrar(t: Terreno) {
    if (!window.confirm(`¿Borrar el terreno "${t.nombre}"? Esta acción no se puede deshacer.`)) return;
    setBorrando(t.id);
    setErrorBorrar(null);
    try {
      await eliminarTerreno(t.id);
      setTerrenos((lista) => (lista ?? []).filter((x) => x.id !== t.id));
    } catch (e) {
      setErrorBorrar(aErrorApi(e));
    } finally {
      setBorrando(null);
    }
  }

  return (
    <Marco usuario={usuario} titulo="Mi terreno" subtitulo="Sus terrenos y dónde quedan. Con ellos calculamos el clima y las recomendaciones de cada lote.">
      {nuevo && <AvisoExito titulo="Terreno guardado" detalle="Ya puede ver su clima y las recomendaciones de siembra." />}
      {error && <TarjetaError mensaje={error.mensaje} codigo={error.codigo} alReintentar={recargar} />}
      {errorBorrar && <TarjetaError mensaje={errorBorrar.mensaje} codigo={errorBorrar.codigo} />}
      {!terrenos && !error && (
        <p role="status" className="text-[15px] text-tinta-suave">
          Cargando sus terrenos…
        </p>
      )}
      {terrenos && terrenos.length === 0 && (
        <section className="flex flex-col items-start gap-3 rounded-2xl border border-borde bg-white p-5">
          <h2 className="text-[17px] font-bold">Aún no ha registrado terrenos</h2>
          <p className="text-[14px] text-tinta-suave">Marque el contorno de su terreno en el mapa: toma un par de minutos.</p>
          <Link href="/lotes/nuevo" className={`${CLASE_BOTON_PRINCIPAL} w-auto`}>
            Registrar mi terreno
          </Link>
        </section>
      )}
      {terrenos && terrenos.length > 0 && (
        <div className="grid gap-[14px] 2xl:grid-cols-[minmax(0,1fr)_420px] 2xl:items-start">
          <MapaTerreno vertices={terrenos[0].vertices} otros={terrenos.slice(1).map((t) => ({ id: t.id, vertices: t.vertices }))} alto="h-[360px] md:h-[480px]" />
          <section aria-label="Terrenos" className="flex flex-col gap-3">
            {terrenos.map((t) => (
              <article key={t.id} className="flex items-center justify-between gap-3 rounded-2xl border border-borde bg-white p-4">
                <div className="min-w-0">
                  <h2 className="truncate text-[16px] font-bold">{t.nombre}</h2>
                  <p className="text-[13px] text-tinta-suave">
                    {formatoArea(t.area_ha)} · {t.riego ? "Con riego" : "Sin riego"} · {t.centroide.lat.toFixed(5)}, {t.centroide.lon.toFixed(5)}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => borrar(t)}
                  disabled={borrando === t.id}
                  className="min-h-11 shrink-0 rounded-[10px] px-3 text-[14px] font-semibold text-roja-texto hover:bg-roja-fondo disabled:opacity-50"
                >
                  {borrando === t.id ? "Borrando…" : "Borrar"}
                </button>
              </article>
            ))}
            <Link href="/lotes/nuevo" className={CLASE_BOTON_PRINCIPAL}>
              Agregar otro terreno
            </Link>
          </section>
        </div>
      )}
    </Marco>
  );
}

function NuevoTerreno({ usuario }: { usuario: UsuarioSesion }) {
  const router = useRouter();
  const { terrenos } = useTerrenos();
  const primero = useSearchParams().get("bienvenida");
  return (
    <Marco
      usuario={usuario}
      titulo={primero ? `¡Bienvenido, ${usuario.nombre}! Registre su terreno` : "Registrar terreno"}
      subtitulo="Con el contorno sabemos dónde queda su terreno y su tamaño, para recomendarle qué sembrar y cuándo."
    >
      <FormularioTerreno otros={terrenos ?? []} alGuardar={() => router.push("/lotes?nuevo=1")} />
    </Marco>
  );
}

export function TerrenosConSesion() {
  return <SesionRequerida>{(usuario) => <ListaTerrenos usuario={usuario} />}</SesionRequerida>;
}

export function NuevoTerrenoConSesion() {
  return <SesionRequerida>{(usuario) => <NuevoTerreno usuario={usuario} />}</SesionRequerida>;
}
