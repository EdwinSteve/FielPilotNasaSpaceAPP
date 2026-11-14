"use client";

// La pantalla de rotación exige sesión: usa el usuario real (GET /usuarios/me) en la navegación
// y le da un botón para cerrar sesión. El plan sigue viniendo de obtenerPlanRotacion.
import { BotonCerrarSesion, SesionRequerida } from "@/modulos/identidad";
import type { PlanRotacion } from "../tipos";
import { PantallaRotacion } from "./PantallaRotacion";

export function RotacionConSesion({ plan }: { plan: PlanRotacion }) {
  return (
    <SesionRequerida>
      {({ nombreCompleto, iniciales, rol, municipio }) => (
        <PantallaRotacion
          plan={{ ...plan, usuario: { nombreCompleto, iniciales, rol, municipio } }}
          acciones={{
            lateral: <BotonCerrarSesion variante="lateral" />,
            movil: <BotonCerrarSesion variante="movil" />,
          }}
        />
      )}
    </SesionRequerida>
  );
}
