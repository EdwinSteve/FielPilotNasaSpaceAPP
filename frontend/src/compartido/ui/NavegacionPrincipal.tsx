// Navegación de la app: barra lateral (completa en escritorio, compacta en tablet)
// y barra de pestañas inferior en móvil.
import Link from "next/link";
import { Icono } from "./Icono";

export type SeccionNav = "inicio" | "terreno" | "rotacion" | "alertas";

interface Props {
  activa: SeccionNav;
  usuario: { nombreCompleto: string; iniciales: string; rol: string; municipio: string };
  /** Lote sobre el que abre "Cultivos y rotación". */
  loteId: string;
  alertasPendientes: number;
}

export function NavegacionPrincipal({ activa, usuario, loteId, alertasPendientes }: Props) {
  // El diseño solo trae el icono resaltado de Inicio; los demás usan su variante normal.
  const secciones = [
    { id: "inicio", nombre: "Inicio", corto: "Inicio", href: "/inicio", icono: "home-blanco", iconoTab: "home-tab" },
    { id: "terreno", nombre: "Mi terreno", corto: "Terreno", href: "/lotes", icono: "pin-nav", iconoTab: "pin-tab" },
    { id: "rotacion", nombre: "Cultivos y rotación", corto: "Cultivos", href: `/lotes/${loteId}/rotacion`, icono: "rotate-nav", iconoTab: "rotate-tab" },
    { id: "alertas", nombre: "Alertas IDEAM", corto: "Alertas", href: "/alertas", icono: "alert-nav", iconoTab: "alert-tab" },
  ] as const;

  return (
    <>
      <aside className="sticky top-0 hidden h-screen w-[72px] shrink-0 flex-col justify-between bg-verde-900 px-[14px] py-5 md:flex 2xl:w-[264px] 2xl:p-6">
        <div className="flex flex-col gap-6 2xl:gap-8">
          <div className="flex items-center justify-center gap-[10px] 2xl:justify-start">
            <span className="rounded-[10px] bg-verde-500 p-[11px] 2xl:p-2">
              <Icono nombre="sprout-blanco" />
            </span>
            <span className="hidden text-[20px] font-bold text-white 2xl:block">Field Shift</span>
          </div>
          <nav aria-label="Principal" className="flex flex-col gap-[6px] 2xl:gap-1">
            {secciones.map((s) => {
              const esActiva = s.id === activa;
              return (
                <Link
                  key={s.id}
                  href={s.href}
                  aria-label={s.nombre}
                  aria-current={esActiva ? "page" : undefined}
                  className={`flex size-11 items-center justify-center gap-3 rounded-[10px] 2xl:h-auto 2xl:w-full 2xl:justify-start 2xl:px-[14px] 2xl:py-3 ${
                    esActiva ? "bg-white/12" : "hover:bg-white/8"
                  }`}
                >
                  <Icono nombre={s.icono} />
                  <span
                    className={`hidden text-[15px] 2xl:block ${
                      esActiva ? "font-semibold text-white" : "font-medium text-verde-niebla"
                    }`}
                  >
                    {s.nombre}
                  </span>
                  {s.id === "alertas" && alertasPendientes > 0 && (
                    <span className="ml-auto hidden rounded-full bg-naranja px-2 py-[2px] text-[12px] font-bold text-white 2xl:block">
                      {alertasPendientes}
                    </span>
                  )}
                </Link>
              );
            })}
          </nav>
        </div>
        <div className="flex items-center justify-center 2xl:justify-start 2xl:gap-3 2xl:rounded-xl 2xl:bg-white/8 2xl:p-3">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-verde-500 text-[14px] font-bold text-white">
            {usuario.iniciales}
          </span>
          <span className="hidden flex-col gap-[2px] 2xl:flex">
            <span className="text-[14px] font-semibold text-white">{usuario.nombreCompleto}</span>
            <span className="text-[12px] text-verde-niebla">
              {usuario.rol} · {usuario.municipio}
            </span>
          </span>
        </div>
      </aside>

      <nav
        aria-label="Principal"
        className="fixed inset-x-0 bottom-0 z-10 flex border border-borde bg-white p-2 md:hidden"
      >
        {secciones.map((s) => {
          const esActiva = s.id === activa;
          return (
            <Link
              key={s.id}
              href={s.href}
              aria-current={esActiva ? "page" : undefined}
              className={`flex flex-1 flex-col items-center gap-1 rounded-[10px] py-[6px] text-[10px] ${
                esActiva ? "bg-verde-100 font-semibold text-verde-700" : "font-medium text-tinta-suave"
              }`}
            >
              <Icono nombre={s.iconoTab} />
              {s.corto}
            </Link>
          );
        })}
      </nav>
    </>
  );
}
