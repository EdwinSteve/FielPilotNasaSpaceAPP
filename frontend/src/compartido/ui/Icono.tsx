// Icono SVG exportado del diseño (public/img/iconos). Decorativo: sin texto alternativo.
interface Props {
  nombre: string;
  /** Tamaño en px; `className` puede cambiarlo por breakpoint (p. ej. "size-4 2xl:size-5"). */
  tam?: number;
  className?: string;
}

export function Icono({ nombre, tam = 20, className = "" }: Props) {
  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      src={`/img/iconos/${nombre}.svg`}
      alt=""
      width={tam}
      height={tam}
      className={`block shrink-0 ${className}`}
    />
  );
}
