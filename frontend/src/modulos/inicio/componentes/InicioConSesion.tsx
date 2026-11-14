"use client";

// La pantalla de inicio exige sesión: muestra el nombre, las iniciales y el municipio del usuario real
// (GET /usuarios/me) y le da un botón para cerrar sesión. El resto del resumen (clima, alertas,
// recomendación) sigue viniendo de obtenerResumenInicio hasta que existan esos endpoints.
import { BotonCerrarSesion, SesionRequerida } from "@/modulos/identidad";
import type { ResumenInicio } from "../tipos";
import { PantallaInicio } from "./PantallaInicio";

export function InicioConSesion({ resumen }: { resumen: ResumenInicio }) {
  return (
    <SesionRequerida>
      {({ nombre, nombreCompleto, iniciales, rol, municipio }) => (
        <PantallaInicio
          resumen={{ ...resumen, usuario: { nombre, nombreCompleto, iniciales, rol, municipio } }}
          acciones={{
            lateral: <BotonCerrarSesion variante="lateral" />,
            movil: <BotonCerrarSesion variante="movil" />,
          }}
        />
      )}
    </SesionRequerida>
  );
}
