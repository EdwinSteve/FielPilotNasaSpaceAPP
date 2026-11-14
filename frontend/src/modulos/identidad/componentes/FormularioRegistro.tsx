"use client";

// Registro del productor: datos de la cuenta y autorización de datos.
// La finca y el lote se registran después, en el módulo fincas.
import Link from "next/link";
import { useState, type FormEvent } from "react";
import { aErrorApi, type ErrorApi } from "@/compartido/api/tipos";
import { AvisoExito, BotonPrincipal, CampoCelular, CampoContrasena, CampoTexto, TarjetaError } from "@/compartido/ui";
import { SelectorUbicacion, type Ubicacion } from "@/modulos/territorio";
import { registrarUsuario } from "../api";
import { usePoliticaVigente } from "../hooks/usePoliticaVigente";
import { CODIGOS, type DatosRegistro, type ErroresFormulario } from "../tipos";
import { sinErrores, validarRegistro } from "../validaciones";
import { MarcoAutenticacion } from "./MarcoAutenticacion";
import { TarjetaConsentimiento } from "./TarjetaConsentimiento";

type Campo = keyof DatosRegistro;

const INICIAL: DatosRegistro = {
  nombre_completo: "",
  celular: "",
  correo: "",
  contrasena: "",
  departamento_residencia: "",
  municipio_residencia: "",
  acepta_politica: false,
};

interface Props {
  alRegistrarse: (nombre: string) => void;
}

export function FormularioRegistro({ alRegistrarse }: Props) {
  const [datos, setDatos] = useState<DatosRegistro>(INICIAL);
  const [errores, setErrores] = useState<ErroresFormulario<Campo>>({});
  const [errorApi, setErrorApi] = useState<ErrorApi | null>(null);
  const [celularDuplicado, setCelularDuplicado] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [registrado, setRegistrado] = useState<string | null>(null);

  const politica = usePoliticaVigente();

  function cambiarUbicacion({ departamento, municipio }: Ubicacion) {
    setDatos((d) => ({ ...d, departamento_residencia: departamento, municipio_residencia: municipio }));
    setErrores((e) => ({ ...e, departamento_residencia: undefined, municipio_residencia: undefined }));
  }

  function cambiar<C extends Campo>(campo: C) {
    return (valor: DatosRegistro[C]) => {
      setDatos((d) => ({ ...d, [campo]: valor }));
      setErrores((e) => ({ ...e, [campo]: undefined }));
      if (campo === "celular") setCelularDuplicado(false);
    };
  }

  /** Lleva cada error conocido del backend al campo que el agricultor tiene que corregir. */
  function mostrarErrorBackend(error: ErrorApi) {
    switch (error.codigo) {
      case CODIGOS.celularDuplicado:
        setCelularDuplicado(true);
        setErrores((e) => ({ ...e, celular: "Ya existe una cuenta con este celular." }));
        return;
      case CODIGOS.correoDuplicado:
        setErrores((e) => ({ ...e, correo: "Este correo ya está registrado en otra cuenta." }));
        return;
      case CODIGOS.consentimientoRequerido:
        setErrores((e) => ({ ...e, acepta_politica: "Debe autorizar el uso de datos para continuar." }));
        return;
      case CODIGOS.politicaDesactualizada:
        setDatos((d) => ({ ...d, acepta_politica: false }));
        setErrores((e) => ({ ...e, acepta_politica: "La política de datos cambió. Léala y autorícela de nuevo." }));
        politica.recargar();
        return;
    }
    if (error.detalles?.length) {
      setErrores((e) => ({ ...e, ...Object.fromEntries(error.detalles!.map((d) => [d.campo, d.mensaje])) }));
      return;
    }
    setErrorApi(error);
  }

  async function alEnviar(e: FormEvent) {
    e.preventDefault();
    setErrorApi(null);
    const encontrados = validarRegistro(datos);
    setErrores(encontrados);
    if (!sinErrores(encontrados)) {
      // Lleva el foco al primer campo con error para que el agricultor lo vea en el celular.
      // Espera a que React pinte los errores: antes de eso ningún campo tiene aria-invalid.
      requestAnimationFrame(() => document.querySelector<HTMLElement>("[aria-invalid='true']")?.focus());
      return;
    }
    if (!politica.politica) {
      setErrorApi({ codigo: "POLITICA_NO_CARGADA", mensaje: "No pudimos cargar la política de datos. Vuelva a intentarlo." });
      return;
    }

    setEnviando(true);
    try {
      const { usuario } = await registrarUsuario(datos, politica.politica.version);
      const primerNombre = usuario.nombre_completo.split(/\s+/)[0];
      setRegistrado(primerNombre);
      alRegistrarse(primerNombre);
    } catch (err) {
      mostrarErrorBackend(aErrorApi(err));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <MarcoAutenticacion
      ancho="amplio"
      titulo="Cree su cuenta"
      subtitulo="Para recomendarle qué sembrar según el clima que viene a su finca y avisarle si se anuncia lluvia fuerte o sequía."
      pie={
        <>
          ¿Ya tiene cuenta?{" "}
          <Link href="/ingresar" className="font-black text-campo underline">
            Ingresar
          </Link>
        </>
      }
    >
      {registrado ? (
        <AvisoExito titulo={`¡Bienvenido, ${registrado}!`} detalle="Su cuenta está lista. Lo llevamos a su inicio…" />
      ) : (
        <form onSubmit={alEnviar} noValidate className="flex flex-col gap-5 lg:grid lg:grid-cols-2 lg:gap-x-6">
          {errorApi && (
            <div className="lg:col-span-2">
              <TarjetaError mensaje={errorApi.mensaje} codigo={errorApi.codigo} />
            </div>
          )}

          <CampoTexto
            id="registro-nombre"
            etiqueta="Nombre y apellidos"
            obligatorio
            valor={datos.nombre_completo}
            alCambiar={cambiar("nombre_completo")}
            error={errores.nombre_completo}
            autoComplete="name"
            placeholder="Ej: Ernesto Ramírez"
            disabled={enviando}
          />

          <CampoCelular
            id="registro-celular"
            valor={datos.celular}
            alCambiar={cambiar("celular")}
            error={errores.celular}
            ayuda="Aquí le avisaremos cuándo sembrar y si viene lluvia o sequía."
            disabled={enviando}
            accionError={
              celularDuplicado && (
                <Link href="/ingresar" className="ml-1 font-black text-campo underline">
                  Ingresar aquí
                </Link>
              )
            }
          />

          <CampoTexto
            id="registro-correo"
            etiqueta="Correo electrónico"
            opcional
            type="email"
            inputMode="email"
            autoComplete="email"
            valor={datos.correo}
            alCambiar={cambiar("correo")}
            error={errores.correo}
            placeholder="Ej: ernesto@correo.com"
            disabled={enviando}
          />

          <CampoContrasena
            id="registro-contrasena"
            valor={datos.contrasena}
            alCambiar={cambiar("contrasena")}
            error={errores.contrasena}
            ayuda="Mínimo 8 caracteres, con letras y números. Por ejemplo, el nombre de su finca y un año."
            autoComplete="new-password"
            disabled={enviando}
          />

          <SelectorUbicacion
            id="registro"
            valor={{ departamento: datos.departamento_residencia, municipio: datos.municipio_residencia }}
            alCambiar={cambiarUbicacion}
            etiquetaDepartamento="Departamento donde vive"
            etiquetaMunicipio="Municipio donde vive"
            errores={{ departamento: errores.departamento_residencia, municipio: errores.municipio_residencia }}
            disabled={enviando}
            className="lg:col-span-2 lg:grid lg:grid-cols-2 lg:gap-x-6"
          />

          <div className="lg:col-span-2">
            <TarjetaConsentimiento
              aceptada={datos.acepta_politica}
              alCambiar={cambiar("acepta_politica")}
              version={politica.politica?.version}
              cargando={politica.cargando}
              errorCarga={politica.error}
              alReintentar={politica.recargar}
              error={errores.acepta_politica}
              disabled={enviando}
            />
          </div>

          <BotonPrincipal type="submit" cargando={enviando} textoCargando="Creando su cuenta…" className="lg:col-span-2">
            Crear mi cuenta
          </BotonPrincipal>
        </form>
      )}
    </MarcoAutenticacion>
  );
}
