"use client";

// La pantalla de alertas exige sesión: usa el usuario real (GET /usuarios/me) en la navegación.
// Las alertas de todos los lotes se consultan desde el navegador para poder actualizarlas y reintentarlas.
import { BotonCerrarSesion, SesionRequerida } from "@/modulos/identidad";
import { useAlertasLotes } from "../hooks/useAlertasLotes";
import type { LoteUbicado, Simulacion } from "../tipos";
import { PantallaAlertas } from "./PantallaAlertas";

interface Props {
  lotes: LoteUbicado[];
  actual: LoteUbicado;
  simular?: Simulacion;
}

export function AlertasConSesion({ lotes, actual, simular }: Props) {
  const { alertasDe, actualizando, actualizar, reintentar } = useAlertasLotes(lotes, simular);

  return (
    <SesionRequerida>
      {({ nombreCompleto, iniciales, rol, municipio }) => (
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
      )}
    </SesionRequerida>
  );
}
