// "Indicador de sequía": lluvia de los últimos 30 días frente a la normal, con NASA POWER (GET /alerts/drought).
// Es un indicador propio, no una alerta oficial del IDEAM: la tarjeta lo dice siempre.
import type { Consulta, IndicadorSequia } from "../tipos";
import { ESCALA_SEQUIA, NIVELES_SEQUIA, TONOS } from "./niveles";
import { AvisoCargando, AvisoFallo, CLASE_TARJETA, Chip, EncabezadoTarjeta, EscalaNiveles } from "./piezas";

export function TarjetaSequia({
  consulta,
  alReintentar,
}: {
  consulta: Consulta<IndicadorSequia>;
  alReintentar: () => void;
}) {
  const nivel = consulta.estado === "ok" ? NIVELES_SEQUIA[consulta.datos.nivel] : null;

  return (
    <section aria-labelledby="titulo-sequia" className={`${CLASE_TARJETA} gap-3 2xl:gap-4`}>
      <EncabezadoTarjeta
        id="titulo-sequia"
        titulo="Indicador de sequía"
        fuente="NASA POWER"
        icono={{ nombre: "sun-18", fondo: "bg-amarilla-fondo" }}
      />
      {consulta.estado === "cargando" && <AvisoCargando texto="Consultando la lluvia de la NASA…" />}
      {consulta.estado === "error" && (
        <AvisoFallo fuente="sequia" fallo={consulta.fallo} alReintentar={alReintentar} />
      )}
      {nivel && (
        <>
          <div role="status" className={`flex flex-col items-start gap-2 rounded-[14px] p-4 ${TONOS[nivel.tono].fondo}`}>
            <Chip tono={nivel.tono} sobreBlanco>
              {nivel.etiqueta}
            </Chip>
            <p className="text-[14px] leading-[1.45] font-semibold 2xl:text-[16px]">{nivel.mensaje}</p>
          </div>
          <EscalaNiveles titulo="Escala del indicador de sequía" nombres={ESCALA_SEQUIA} vigente={nivel.escala} />
        </>
      )}
      <p className="text-[12px] leading-[1.45] text-tinta-suave">
        Compara la lluvia de los últimos 30 días con la lluvia normal de esas fechas en tu ubicación. Es un
        indicador propio calculado con datos de la NASA: no es una alerta oficial del IDEAM.
      </p>
    </section>
  );
}
