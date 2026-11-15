"use client";

// La pantalla de alertas exige sesión: usa el usuario real (GET /usuarios/me) en la navegación.
// Los lotes son los terrenos del productor y sus alertas se consultan desde el navegador
// (GET /alerts/flood y /alerts/drought) para poder actualizarlas y reintentarlas.
import Link from "next/link";
import type { ReactNode } from "react";
import { CLASE_BOTON_PRINCIPAL, Spinner, TarjetaError } from "@/compartido/ui";
import { BotonCerrarSesion, SesionRequerida, type UsuarioSesion } from "@/modulos/identidad";
import { useAlertasLotes } from "../hooks/useAlertasLotes";
import { useLotesUbicados } from "../hooks/useLotesUbicados";
import type { LoteUbicado, Simulacion } from "../tipos";
import { PantallaAlertas } from "./PantallaAlertas";

interface Props {
  /** ?lote=<id>; sin él, el primer terreno. */
  loteId?: string;
  simular?: Simulacion;
}

function Marco({ children }: { children: ReactNode }) {
  return (
    <main className="flex min-h-dvh items-center justify-center bg-fondo p-4">
      <div className="flex w-full max-w-md flex-col gap-4">{children}</div>
    </main>
  );
}

function Aviso({ titulo, texto, enlace }: { titulo: string; texto: string; enlace: { href: string; texto: string } }) {
  return (
    <Marco>
      <section className="flex flex-col gap-3 rounded-2xl border border-borde bg-white p-5 text-left">
        <h1 className="text-[18px] font-bold">{titulo}</h1>
        <p className="text-[15px] leading-[1.45] text-tinta-suave">{texto}</p>
        <Link href={enlace.href} className={CLASE_BOTON_PRINCIPAL}>
          {enlace.texto}
        </Link>
      </section>
    </Marco>
  );
}

function Alertas({
  usuario,
  lotes,
  actual,
  simular,
}: {
  usuario: UsuarioSesion;
  lotes: LoteUbicado[];
  actual: LoteUbicado;
  simular?: Simulacion;
}) {
  const { alertasDe, actualizando, actualizar, reintentar } = useAlertasLotes(lotes, simular);
  const { nombreCompleto, iniciales, rol, municipio } = usuario;
  return (
    <PantallaAlertas
      usuario={{ nombreCompleto, iniciales, rol, municipio }}
      lotes={lotes}
      actual={actual}
      alertasDe={alertasDe}
      actualizando={actualizando}
      alActualizar={actualizar}
      alReintentar={reintentar}
      acciones={{
        lateral: <BotonCerrarSesion variante="lateral" />,
        movil: <BotonCerrarSesion variante="movil" />,
      }}
    />
  );
}

function AlertasDelUsuario({ usuario, loteId, simular }: Props & { usuario: UsuarioSesion }) {
  const { estado, reintentar } = useLotesUbicados();

  if (estado.fase === "cargando") {
    return (
      <Marco>
        <p role="status" className="flex items-center justify-center gap-2 text-[15px] text-tinta-suave">
          <Spinner className="size-5 text-verde-700" />
          Buscando sus terrenos…
        </p>
      </Marco>
    );
  }
  if (estado.fase === "error") {
    return (
      <Marco>
        <TarjetaError mensaje={estado.error.mensaje} codigo={estado.error.codigo} alReintentar={reintentar} />
      </Marco>
    );
  }

  const { lotes } = estado;
  if (lotes.length === 0) {
    return (
      <Aviso
        titulo="Todavía no tiene terrenos"
        texto="Dibuje su terreno en el mapa y le avisaremos de las alertas de ríos y quebradas del IDEAM y de la sequía en su zona."
        enlace={{ href: "/lotes/nuevo", texto: "Dibujar mi terreno" }}
      />
    );
  }
  const actual = loteId ? lotes.find((l) => l.id === loteId) : lotes[0];
  if (!actual) {
    return (
      <Aviso
        titulo="No encontramos ese lote"
        texto="Puede que lo haya borrado o que sea de otra cuenta."
        enlace={{ href: "/alertas", texto: "Ver las alertas de mis terrenos" }}
      />
    );
  }
  return <Alertas usuario={usuario} lotes={lotes} actual={actual} simular={simular} />;
}

export function AlertasConSesion({ loteId, simular }: Props) {
  return (
    <SesionRequerida>
      {(usuario) => <AlertasDelUsuario usuario={usuario} loteId={loteId} simular={simular} />}
    </SesionRequerida>
  );
}
