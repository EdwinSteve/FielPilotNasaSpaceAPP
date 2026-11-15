"use client";

// La pantalla de rotación exige sesión: usa el usuario real (GET /usuarios/me) en la navegación y le
// da un botón para cerrar sesión. El plan, los lotes y la elección vienen del backend (usePlanRotacion).
import { BotonCerrarSesion, SesionRequerida, type UsuarioSesion } from "@/modulos/identidad";
import { usePlanRotacion } from "../hooks/usePlanRotacion";
import { EstadoRotacion } from "./EstadoRotacion";
import { PantallaRotacion } from "./PantallaRotacion";

function PlanDelLote({ loteId, usuario }: { loteId: string; usuario: UsuarioSesion }) {
  const { estado, reintentar, elegir, eligiendo, errorSeleccion } = usePlanRotacion(loteId);
  if (estado.fase !== "listo") return <EstadoRotacion estado={estado} alReintentar={reintentar} />;

  const { nombreCompleto, iniciales, rol, municipio } = usuario;
  return (
    <PantallaRotacion
      plan={{ ...estado.plan, usuario: { nombreCompleto, iniciales, rol, municipio } }}
      seleccion={{ alElegir: elegir, eligiendo, error: errorSeleccion }}
      acciones={{
        lateral: <BotonCerrarSesion variante="lateral" />,
        movil: <BotonCerrarSesion variante="movil" />,
      }}
    />
  );
}

export function RotacionConSesion({ loteId }: { loteId: string }) {
  return <SesionRequerida>{(usuario) => <PlanDelLote loteId={loteId} usuario={usuario} />}</SesionRequerida>;
}
