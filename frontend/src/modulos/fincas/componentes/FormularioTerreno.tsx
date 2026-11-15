"use client";

// Registro del terreno: nombre, riego, municipio y el contorno dibujado sobre el mapa satelital.
// Al marcar 3 esquinas se sugiere el municipio (la cabecera más cercana al centro del terreno);
// el productor lo puede cambiar.
import dynamic from "next/dynamic";
import { useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { aErrorApi, type ErrorApi } from "@/compartido/api/tipos";
import { AyudaCampo, BotonPrincipal, CampoTexto, CLASE_BOTON_SECUNDARIO, TarjetaError } from "@/compartido/ui";
import { municipioCercano, SelectorUbicacion, useMunicipios, type Ubicacion } from "@/modulos/territorio";
import { crearTerreno } from "../api";
import { formatoArea, medir, problemaDelContorno } from "../geometria";
import type { Terreno, Vertice } from "../tipos";
import type { Enfoque } from "./MapaTerreno";

const MapaTerreno = dynamic(() => import("./MapaTerreno").then((m) => m.MapaTerreno), {
  ssr: false,
  loading: () => (
    <div className="flex h-[420px] w-full items-center justify-center rounded-2xl border border-borde bg-fondo text-[14px] text-tinta-suave">
      Cargando el mapa…
    </div>
  ),
});

interface Errores {
  nombre?: string;
  municipio?: string;
  contorno?: string;
}

interface Props {
  alGuardar: (terreno: Terreno) => void;
  /** Otros terrenos del productor, de referencia en el mapa. */
  otros?: Terreno[];
}

export function FormularioTerreno({ alGuardar, otros = [] }: Props) {
  const [nombre, setNombre] = useState("");
  const [riego, setRiego] = useState(false);
  const [ubicacion, setUbicacion] = useState<Ubicacion>({ departamento: "", municipio: "" });
  const [vertices, setVertices] = useState<Vertice[]>([]);
  const [enfoque, setEnfoque] = useState<Enfoque | undefined>();
  const [errores, setErrores] = useState<Errores>({});
  const [errorGeneral, setErrorGeneral] = useState<ErrorApi | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [ubicando, setUbicando] = useState(false);
  const [intentoGuardar, setIntentoGuardar] = useState(false);
  const municipioElegidoAMano = useRef(false);
  const municipios = useMunicipios(ubicacion.departamento);

  const medida = useMemo(() => medir(vertices), [vertices]);
  const problema = problemaDelContorno(vertices);

  // Al cerrar el contorno, sugiere el municipio de la cabecera más cercana al centro.
  const centroide = medida?.centroide;
  useEffect(() => {
    if (!centroide || municipioElegidoAMano.current || problema) return;
    let vigente = true;
    municipioCercano(centroide.lat, centroide.lon)
      .then((m) => {
        if (vigente && !municipioElegidoAMano.current) setUbicacion({ departamento: m.codigo.slice(0, 2), municipio: m.codigo });
      })
      .catch(() => undefined); // sin sugerencia: el productor lo elige
    return () => {
      vigente = false;
    };
    // Solo cuando cambia el contorno, no en cada render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [centroide?.lat, centroide?.lon, problema]);

  function cambiarUbicacion(u: Ubicacion) {
    municipioElegidoAMano.current = true;
    setUbicacion(u);
    setErrores((e) => ({ ...e, municipio: undefined }));
    // Sin contorno todavía: lleva el mapa a la cabecera del municipio elegido.
    const m = municipios.lista.find((x) => x.codigo === u.municipio);
    if (vertices.length === 0 && m?.lat != null && m?.lon != null) {
      setEnfoque({ centro: { lat: m.lat, lon: m.lon }, zoom: 14, clave: Date.now() });
    }
  }

  function usarMiUbicacion() {
    if (!navigator.geolocation) {
      setErrores((e) => ({ ...e, contorno: "Su navegador no permite ubicarlo. Busque el terreno moviendo el mapa." }));
      return;
    }
    setUbicando(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setUbicando(false);
        setEnfoque({ centro: { lat: pos.coords.latitude, lon: pos.coords.longitude }, zoom: 17, clave: Date.now() });
      },
      () => {
        setUbicando(false);
        setErrores((e) => ({ ...e, contorno: "No pudimos ubicarlo. Active el GPS o busque el terreno moviendo el mapa." }));
      },
      { enableHighAccuracy: true, timeout: 10_000 },
    );
  }

  function cambiarVertices(v: Vertice[]) {
    setVertices(v);
    setErrores((e) => ({ ...e, contorno: undefined }));
  }

  async function guardar(ev: FormEvent) {
    ev.preventDefault();
    setIntentoGuardar(true);
    const nuevos: Errores = {
      nombre: nombre.trim() ? undefined : 'Póngale un nombre, por ejemplo "Lote de arriba".',
      municipio: ubicacion.municipio ? undefined : "Elija el municipio donde queda el terreno.",
      contorno: problema ?? undefined,
    };
    setErrores(nuevos);
    if (nuevos.nombre || nuevos.municipio || nuevos.contorno) return;

    setEnviando(true);
    setErrorGeneral(null);
    try {
      alGuardar(await crearTerreno({ nombre: nombre.trim(), municipio: ubicacion.municipio, riego, vertices }));
    } catch (e) {
      const error = aErrorApi(e);
      const porCampo: Errores = {};
      for (const d of error.detalles ?? []) {
        if (d.campo === "nombre") porCampo.nombre = d.mensaje;
        else if (d.campo === "municipio") porCampo.municipio = d.mensaje;
        else if (d.campo.startsWith("vertices")) porCampo.contorno = d.mensaje;
      }
      if (Object.keys(porCampo).length) setErrores(porCampo);
      else setErrorGeneral(error);
    } finally {
      setEnviando(false);
    }
  }

  const avisoContorno = errores.contorno ?? (intentoGuardar || vertices.length >= 3 ? problema : null);

  return (
    <form onSubmit={guardar} noValidate className="flex flex-col gap-5">
      <section aria-labelledby="titulo-contorno" className="flex flex-col gap-3 rounded-2xl border border-borde bg-white p-4 md:p-5">
        <div className="flex flex-col gap-1">
          <h2 id="titulo-contorno" className="text-[17px] font-bold text-tinta">
            1. Marque el contorno de su terreno
          </h2>
          <p className="text-[14px] text-tinta-suave">
            Busque su terreno en la foto satelital y toque cada esquina, en orden, siguiendo el borde. El primer punto queda en
            amarillo.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" onClick={usarMiUbicacion} disabled={ubicando} className={`${CLASE_BOTON_SECUNDARIO} w-auto`}>
            {ubicando ? "Ubicando…" : "Estoy en el terreno: usar mi ubicación"}
          </button>
          <button
            type="button"
            onClick={() => cambiarVertices(vertices.slice(0, -1))}
            disabled={vertices.length === 0}
            className={`${CLASE_BOTON_SECUNDARIO} w-auto disabled:opacity-50`}
          >
            Deshacer último punto
          </button>
          <button
            type="button"
            onClick={() => cambiarVertices([])}
            disabled={vertices.length === 0}
            className={`${CLASE_BOTON_SECUNDARIO} w-auto disabled:opacity-50`}
          >
            Borrar todo
          </button>
        </div>
        <MapaTerreno
          vertices={vertices}
          alCambiar={cambiarVertices}
          otros={otros.map((t) => ({ id: t.id, vertices: t.vertices }))}
          enfoque={enfoque}
          invalido={Boolean(avisoContorno)}
        />
        <p role="status" aria-live="polite" className="text-[14px] font-medium text-tinta">
          {vertices.length === 0
            ? "Aún no ha marcado puntos."
            : `${vertices.length} ${vertices.length === 1 ? "punto marcado" : "puntos marcados"}${
                medida && !problema ? ` · Área: ${formatoArea(medida.areaHa)}` : ""
              }`}
        </p>
        <AyudaCampo id="contorno" error={avisoContorno ?? undefined} />
      </section>

      <section aria-labelledby="titulo-datos" className="flex flex-col gap-5 rounded-2xl border border-borde bg-white p-4 md:p-5">
        <h2 id="titulo-datos" className="text-[17px] font-bold text-tinta">
          2. Datos del terreno
        </h2>
        <CampoTexto
          id="terreno-nombre"
          etiqueta="Nombre del terreno"
          obligatorio
          valor={nombre}
          alCambiar={(v) => {
            setNombre(v);
            setErrores((e) => ({ ...e, nombre: undefined }));
          }}
          placeholder="Ej.: Lote de arriba"
          maxLength={60}
          error={errores.nombre}
        />
        <SelectorUbicacion
          id="terreno"
          valor={ubicacion}
          alCambiar={cambiarUbicacion}
          etiquetaMunicipio="Municipio donde queda el terreno"
          errores={{ municipio: errores.municipio }}
          className="md:grid md:grid-cols-2 md:gap-5"
        />
        {medida && !problema && !municipioElegidoAMano.current && ubicacion.municipio && (
          <p className="-mt-3 text-[13px] text-tinta-suave">Sugerimos el municipio por la ubicación del terreno. Cámbielo si no es.</p>
        )}
        <label className="flex min-h-12 cursor-pointer items-center gap-3 rounded-[10px] border border-borde-campo px-[14px]">
          <input type="checkbox" checked={riego} onChange={(e) => setRiego(e.target.checked)} className="size-5 accent-verde-700" />
          <span className="text-[15px] font-medium text-tinta">El terreno tiene riego</span>
        </label>
      </section>

      {errorGeneral && <TarjetaError mensaje={errorGeneral.mensaje} codigo={errorGeneral.codigo} />}
      <BotonPrincipal type="submit" cargando={enviando} textoCargando="Guardando terreno…">
        Guardar terreno
      </BotonPrincipal>
    </form>
  );
}
