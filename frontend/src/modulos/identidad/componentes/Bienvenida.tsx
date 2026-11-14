// Primera pantalla: qué hace la app y la entrada al registro o al ingreso.
// Mismos estándares del diseño de Figma: fondo, tarjetas blancas de 16 px con borde de 1 px,
// iconos sobre verde-100, botones de 10 px en verde-700.
import Image from "next/image";
import Link from "next/link";
import type { ReactNode } from "react";
import {
  CLASE_BOTON_PRINCIPAL,
  CLASE_BOTON_SECUNDARIO,
  IconoCampana,
  IconoNube,
  IconoRotacion,
  IconoSatelite,
  Marca,
  NOMBRE_APP,
} from "@/compartido/ui";

/** Etiqueta sobre la imagen: fondo blanco translúcido para leerse sobre cualquier zona de la ilustración. */
function Chip({ icono, texto, punto, soloEscritorio }: { icono?: ReactNode; texto: string; punto?: boolean; soloEscritorio?: boolean }) {
  return (
    <span
      className={`${soloEscritorio ? "hidden lg:inline-flex" : "inline-flex"} min-w-0 items-center gap-1.5 rounded-full bg-white/90 px-3 py-1.5 text-[12px] font-semibold text-verde-700 shadow-sm backdrop-blur`}
    >
      {punto && <span className="h-2 w-2 shrink-0 rounded-full bg-verde-500" aria-hidden="true" />}
      {icono && <span className="shrink-0">{icono}</span>}
      <span className="truncate">{texto}</span>
    </span>
  );
}

interface PropsBeneficio {
  icono: ReactNode;
  /** Celular y tablet: tarjeta compacta. */
  tituloCorto: string;
  textoCorto: string;
  /** Escritorio: fila con más detalle. */
  titulo: string;
  texto: string;
}

/** Cada versión se oculta con display:none en el otro tamaño, así el lector de pantalla solo lee la visible. */
function Beneficio({ icono, tituloCorto, textoCorto, titulo, texto }: PropsBeneficio) {
  return (
    <li className="rounded-2xl border border-borde bg-white px-1.5 py-3 sm:px-3 lg:p-4">
      <div className="flex h-full flex-col items-center gap-1.5 text-center lg:hidden">
        <span className="flex size-9 items-center justify-center rounded-[10px] bg-verde-100 text-verde-700">{icono}</span>
        <span className="text-[13px] leading-tight font-semibold text-tinta min-[360px]:text-[14px]">{tituloCorto}</span>
        <span className="text-[12px] leading-snug text-tinta-suave min-[360px]:text-[13px]">{textoCorto}</span>
      </div>
      <div className="hidden items-start gap-4 text-left lg:flex">
        <span className="flex size-11 shrink-0 items-center justify-center rounded-[10px] bg-verde-100 text-verde-700">{icono}</span>
        <div>
          <p className="text-[16px] font-semibold text-tinta">{titulo}</p>
          <p className="mt-0.5 text-[14px] leading-snug text-tinta-suave">{texto}</p>
        </div>
      </div>
    </li>
  );
}

export function Bienvenida() {
  return (
    <main className="min-h-dvh bg-fondo pt-[env(safe-area-inset-top)] pb-[env(safe-area-inset-bottom)] text-tinta">
      <div className="mx-auto grid w-full max-w-[430px] gap-8 px-4 py-8 sm:max-w-xl sm:px-6 lg:min-h-dvh lg:max-w-6xl lg:grid-cols-2 lg:items-center lg:gap-14 lg:px-12">
        {/* Fondo verde de respaldo: si la imagen no carga, la tarjeta sigue viéndose bien */}
        <figure className="relative h-[330px] w-full overflow-hidden rounded-2xl bg-verde-900 sm:h-[380px] lg:h-[600px]">
          <Image
            src="/img/bienvenida-1-finca-desde-el-espacio.webp"
            alt="Un satélite de la NASA escanea las terrazas de cultivo de las montañas colombianas al amanecer mientras un campesino mira su finca."
            fill
            priority
            sizes="(min-width: 1024px) 50vw, 100vw"
            // En celular la tarjeta recorta los lados: se encuadra para dejar a la vista al campesino y al satélite
            className="object-cover object-[80%_50%]"
          />
          <div className="pointer-events-none absolute inset-x-0 top-0 h-24 bg-linear-to-b from-verde-900/70 to-transparent" />
          <div className="pointer-events-none absolute inset-x-0 bottom-0 h-24 bg-linear-to-t from-verde-900/70 to-transparent" />
          <figcaption className="absolute inset-x-3.5 top-3.5 flex items-center justify-between gap-2">
            <Chip icono={<IconoSatelite className="h-4 w-4" />} texto="Datos de la NASA" />
            {/* En celular esta esquina queda libre para que se vea el satélite */}
            <Chip soloEscritorio icono={<IconoNube className="h-4 w-4" />} texto="Pronóstico IDEAM" />
          </figcaption>
          <div className="absolute inset-x-3.5 bottom-3.5 flex items-center gap-2">
            <Chip punto texto="Su lote, visto desde el espacio" />
          </div>
        </figure>

        <div className="flex flex-col items-center text-center lg:items-start lg:text-left">
          <Marca tam="grande" />
          <h1 className="mt-4 text-[26px] leading-tight font-bold md:text-[30px] lg:text-[40px]">
            Siembre lo que mejor se adapta al clima que viene
          </h1>
          <p className="mt-3 max-w-[460px] text-[15px] leading-relaxed text-tinta-suave md:text-[16px]">
            Miramos su lote desde el espacio y le decimos qué cultivo le puede dar mejor resultado esta temporada y las
            siguientes.
          </p>

          <ul
            className="mt-6 grid w-full grid-cols-3 gap-1.5 min-[360px]:gap-2 lg:grid-cols-1 lg:gap-3"
            aria-label={`Lo que ${NOMBRE_APP} hace por usted`}
          >
            <Beneficio
              icono={<IconoNube className="size-5" />}
              tituloCorto="Clima"
              textoCorto="De su lote"
              titulo="Clima de su lote"
              texto="Lluvia, temperatura y humedad del suelo de su terreno, medidas por satélites de la NASA."
            />
            <Beneficio
              icono={<IconoCampana className="size-5" />}
              tituloCorto="Alertas"
              textoCorto="Lluvia y sequía"
              titulo="Alertas tempranas"
              texto="Le avisamos a su celular cuando el IDEAM pronostica lluvias fuertes o sequía en su municipio."
            />
            <Beneficio
              icono={<IconoRotacion className="size-5" />}
              tituloCorto="Rotación de cultivos"
              textoCorto="Qué sembrar"
              titulo="Rotación de cultivos"
              texto="Qué sembrar en las próximas 2 o 3 temporadas para cuidar su suelo y adaptarse al clima que viene."
            />
          </ul>

          <div className="mt-6 flex w-full flex-col gap-3 lg:flex-row">
            <Link href="/registro" className={`${CLASE_BOTON_PRINCIPAL} lg:flex-1`}>
              Crear mi cuenta
            </Link>
            <Link href="/ingresar" className={`${CLASE_BOTON_SECUNDARIO} lg:flex-1`}>
              Ya tengo cuenta
            </Link>
          </div>

          <p className="mt-5 inline-flex items-center gap-2 rounded-full bg-verde-100 px-4 py-2 text-[13px] font-medium text-verde-700">
            <IconoSatelite className="h-4 w-4 shrink-0" />
            Datos de la NASA y del IDEAM para el campo colombiano
          </p>
        </div>
      </div>
    </main>
  );
}
