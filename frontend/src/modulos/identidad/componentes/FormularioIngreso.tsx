"use client";

// Inicio de sesión con celular y contraseña.
import Link from "next/link";
import { useState, type FormEvent } from "react";
import { aErrorApi, type ErrorApi } from "@/compartido/api/tipos";
import { AvisoExito, BotonPrincipal, CampoCelular, CampoContrasena, TarjetaError } from "@/compartido/ui";
import { iniciarSesion } from "../api";
import { CODIGOS, type DatosIngreso, type ErroresFormulario } from "../tipos";
import { sinErrores, validarIngreso } from "../validaciones";
import { MarcoAutenticacion } from "./MarcoAutenticacion";

interface Props {
  alIngresar: () => void;
}

export function FormularioIngreso({ alIngresar }: Props) {
  const [datos, setDatos] = useState<DatosIngreso>({ celular: "", contrasena: "" });
  const [errores, setErrores] = useState<ErroresFormulario<keyof DatosIngreso>>({});
  const [errorApi, setErrorApi] = useState<ErrorApi | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [ingresado, setIngresado] = useState(false);

  const cambiar = (campo: keyof DatosIngreso) => (valor: string) => {
    setDatos((d) => ({ ...d, [campo]: valor }));
    setErrores((e) => ({ ...e, [campo]: undefined }));
  };

  async function alEnviar(e: FormEvent) {
    e.preventDefault();
    const encontrados = validarIngreso(datos);
    setErrores(encontrados);
    if (!sinErrores(encontrados)) return;

    setEnviando(true);
    setErrorApi(null);
    try {
      await iniciarSesion(datos);
      setIngresado(true);
      alIngresar();
    } catch (err) {
      const error = aErrorApi(err);
      // Credenciales malas: se marca la contraseña, que es lo que el agricultor puede corregir
      if (error.codigo === CODIGOS.credencialesInvalidas) {
        setErrores({ contrasena: "El celular o la contraseña no coinciden. Revíselos e inténtelo de nuevo." });
      } else {
        setErrorApi(error);
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <MarcoAutenticacion
      titulo="Ingrese a SURCO"
      subtitulo="Use el celular y la contraseña con que creó su cuenta."
      pie={
        <>
          ¿Aún no tiene cuenta?{" "}
          <Link href="/registro" className="font-black text-campo underline">
            Crear mi cuenta
          </Link>
        </>
      }
    >
      {ingresado ? (
        <AvisoExito titulo="Ya inició sesión" detalle="Lo llevamos a su inicio…" />
      ) : (
        <form onSubmit={alEnviar} noValidate className="flex flex-col gap-5">
          {errorApi && <TarjetaError mensaje={errorApi.mensaje} codigo={errorApi.codigo} />}

          <CampoCelular
            id="ingresar-celular"
            valor={datos.celular}
            alCambiar={cambiar("celular")}
            error={errores.celular}
            ayuda="El mismo número con que se registró."
            disabled={enviando}
          />

          <CampoContrasena
            id="ingresar-contrasena"
            valor={datos.contrasena}
            alCambiar={cambiar("contrasena")}
            error={errores.contrasena}
            autoComplete="current-password"
            disabled={enviando}
          />

          <BotonPrincipal type="submit" cargando={enviando} textoCargando="Ingresando…">
            Ingresar
          </BotonPrincipal>
        </form>
      )}
    </MarcoAutenticacion>
  );
}
