// "Ríos y quebradas": la alerta hidrológica vigente del IDEAM para la subzona del lote (GET /alerts/flood).
import { Icono } from "@/compartido/ui/Icono";
import { formatearConsulta, formatearUbicacion } from "../respuestas";
import type { AlertaRios, Consulta } from "../tipos";
import { ESCALA_RIOS, NIVELES_RIOS, TONOS } from "./niveles";
import { AvisoCargando, AvisoFallo, CLASE_TARJETA, EncabezadoTarjeta, EscalaNiveles } from "./piezas";

export function TarjetaRios({ consulta, alReintentar }: { consulta: Consulta<AlertaRios>; alReintentar: () => void }) {
  return (
    <section aria-labelledby="titulo-rios" className={`${CLASE_TARJETA} gap-[14px] 2xl:gap-[18px]`}>
      <EncabezadoTarjeta
        id="titulo-rios"
        titulo="Ríos y quebradas"
        fuente="IDEAM"
        icono={{ nombre: "waves", fondo: "bg-lluvia-fondo" }}
      />
      {consulta.estado === "cargando" && <AvisoCargando texto="Consultando al IDEAM…" />}
      {consulta.estado === "error" && (
        <AvisoFallo fuente="rios" fallo={consulta.fallo} alReintentar={alReintentar} />
      )}
      {consulta.estado === "ok" && <AlertaVigente alerta={consulta.datos} />}
    </section>
  );
}

function AlertaVigente({ alerta }: { alerta: AlertaRios }) {
  const nivel = NIVELES_RIOS[alerta.nivel];
  const tono = TONOS[nivel.tono];
  const datos = [
    ...(alerta.subzona ? [{ nombre: "Zona (subzona hidrográfica)", valor: alerta.subzona }] : []),
    ...(alerta.tipo ? [{ nombre: "Tipo", valor: alerta.tipo }] : []),
    { nombre: "Ubicación consultada", valor: formatearUbicacion(alerta.lat, alerta.lon) },
    { nombre: "Consultado", valor: formatearConsulta(alerta.consultadoEn) },
  ];

  return (
    <>
      <div
        role={nivel.escala >= 2 ? "alert" : "status"}
        className={`flex flex-col gap-3 rounded-[14px] p-4 md:flex-row md:items-center md:gap-5 2xl:p-6 ${tono.fondo}`}
      >
        <span className="flex size-11 items-center justify-center rounded-[14px] bg-white md:size-[68px]">
          {nivel.iconoMovil ? (
            <>
              <Icono nombre={nivel.iconoMovil} tam={24} className="md:hidden" />
              <Icono nombre={nivel.icono} tam={36} className="hidden md:block" />
            </>
          ) : (
            <Icono nombre={nivel.icono} tam={22} />
          )}
        </span>
        <div className="flex min-w-0 flex-1 flex-col gap-[6px]">
          <p className={`text-[12px] font-bold 2xl:text-[13px] ${tono.texto}`}>{nivel.etiqueta}</p>
          <p className="text-[18px] leading-[1.45] font-bold 2xl:text-[24px]">{nivel.titulo}</p>
          {nivel.accion && <p className="text-[14px] leading-[1.45] 2xl:text-[16px]">{nivel.accion}</p>}
        </div>
      </div>

      <dl className="flex flex-col gap-[10px] md:flex-row md:flex-wrap md:gap-x-10 md:gap-y-3">
        {datos.map((d) => (
          <div key={d.nombre} className="flex flex-col gap-[2px]">
            <dt className="text-[11px] font-medium text-tinta-suave">{d.nombre}</dt>
            <dd className="text-[14px] font-semibold">{d.valor}</dd>
          </div>
        ))}
      </dl>

      <EscalaNiveles titulo="Escala de alertas del IDEAM" nombres={ESCALA_RIOS} vigente={nivel.escala} />
    </>
  );
}
