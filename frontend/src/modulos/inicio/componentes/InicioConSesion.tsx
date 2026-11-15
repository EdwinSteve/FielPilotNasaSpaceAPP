"use client";

// La pantalla de inicio exige sesión: muestra el nombre, las iniciales y el municipio del usuario real
// (GET /usuarios/me) y le da un botón para cerrar sesión. El clima, el pronóstico y los avisos vienen
// del backend (GET /clima/resumen) para el municipio del usuario.
import { TarjetaError } from "@/compartido/ui";
import { BotonCerrarSesion, SesionRequerida, type UsuarioSesion } from "@/modulos/identidad";
import { useResumenInicio } from "../hooks/useResumenInicio";
import { PantallaInicio } from "./PantallaInicio";

export function InicioConSesion() {
  return <SesionRequerida>{(usuario) => <InicioDe usuario={usuario} />}</SesionRequerida>;
}

function InicioDe({ usuario }: { usuario: UsuarioSesion }) {
  const { datos, error, reintentar } = useResumenInicio();

  if (!datos) {
    return (
      <main className="flex min-h-dvh items-center justify-center bg-fondo p-4">
        <div className="w-full max-w-md">
          {error ? (
            <TarjetaError mensaje={error.mensaje} codigo={error.codigo} alReintentar={reintentar} />
          ) : (
            <p role="status" className="text-center text-[15px] text-tinta-suave">
              Consultando el clima de {usuario.municipio}…
            </p>
          )}
        </div>
      </main>
    );
  }

  const { nombre, nombreCompleto, iniciales, rol, municipio } = usuario;
  return (
    <PantallaInicio
      resumen={{ ...datos, usuario: { nombre, nombreCompleto, iniciales, rol, municipio } }}
      acciones={{
        lateral: <BotonCerrarSesion variante="lateral" />,
        movil: <BotonCerrarSesion variante="movil" />,
      }}
    />
  );
}
