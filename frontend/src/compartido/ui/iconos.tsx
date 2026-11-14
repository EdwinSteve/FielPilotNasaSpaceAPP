// Iconos SVG pequeños usados en formularios y avisos.

interface PropsIcono {
  className?: string;
}

export function IconoAlerta({ className = "w-4 h-4" }: PropsIcono) {
  return (
    <svg className={`${className} shrink-0`} fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
      <path
        fillRule="evenodd"
        d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
        clipRule="evenodd"
      />
    </svg>
  );
}

export function IconoVerificado({ className = "w-6 h-6" }: PropsIcono) {
  return (
    <svg className={`${className} shrink-0`} fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
      <path
        fillRule="evenodd"
        d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
        clipRule="evenodd"
      />
    </svg>
  );
}

export function Spinner({ className = "w-5 h-5" }: PropsIcono) {
  return (
    <svg className={`${className} animate-spin`} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
    </svg>
  );
}

export function IconoGota({ className = "w-6 h-6" }: PropsIcono) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path strokeLinejoin="round" d="M12 3s6 6.5 6 11a6 6 0 01-12 0c0-4.5 6-11 6-11z" />
    </svg>
  );
}

export function IconoBrote({ className = "w-6 h-6" }: PropsIcono) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path strokeLinecap="round" d="M12 21v-9" />
      <path strokeLinejoin="round" d="M12 12C12 8 9 5 4 5c0 4 3 7 8 7zM12 10c0-3 2.5-5.5 7-5.5 0 3.5-2.5 5.5-7 5.5z" />
    </svg>
  );
}

export function IconoNube({ className = "w-6 h-6" }: PropsIcono) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path strokeLinejoin="round" d="M7 18a4 4 0 01-.5-7.97A5.5 5.5 0 0117 9a4.5 4.5 0 01.5 9H7z" />
      <path strokeLinecap="round" d="M9 21l1-2M13 21l1-2" />
    </svg>
  );
}

export function IconoCampana({ className = "w-6 h-6" }: PropsIcono) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path strokeLinejoin="round" d="M6 16V11a6 6 0 1112 0v5l1.5 2h-15L6 16z" />
      <path strokeLinecap="round" d="M10 21h4" />
    </svg>
  );
}

/** Dos flechas en círculo: la rotación de cultivos temporada tras temporada. */
export function IconoRotacion({ className = "w-6 h-6" }: PropsIcono) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M4 12a8 8 0 0113.7-5.6M20 12a8 8 0 01-13.7 5.6" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M18 3v3.5h-3.5M6 21v-3.5h3.5" />
    </svg>
  );
}

export function IconoSatelite({ className = "w-5 h-5" }: PropsIcono) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <path strokeLinejoin="round" d="M7 10l3-3 7 7-3 3-7-7z" />
      <path strokeLinecap="round" d="M4 7l3 3M10 4l3 3M14 17l3 3M17 14l3 3M5 19a4 4 0 014-4" />
    </svg>
  );
}
