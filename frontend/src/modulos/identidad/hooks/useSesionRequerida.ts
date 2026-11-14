"use client";

// Protege una pantalla: sin sesión (o si ya no se puede renovar) manda a /ingresar.
// Con sesión, trae el perfil de la ruta protegida GET /usuarios/me y el nombre de su municipio.
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { haySesion } from "@/compartido/api/sesion";
import { aErrorApi, type ErrorApi } from "@/compartido/api/tipos";
import { listarMunicipios } from "@/modulos/territorio";
import { obtenerMiPerfil } from "../api";
import type { Usuario } from "../tipos";

/** El usuario de la sesión, listo para mostrar en la navegación y los saludos. */
export interface UsuarioSesion {
  id: string;
  nombre: string;
  nombreCompleto: string;
  iniciales: string;
  rol: string;
  municipio: string;
}

const SESION_TERMINADA = new Set(["AUTH_NO_AUTENTICADO", "AUTH_TOKEN_INVALIDO", "AUTH_REFRESH_INVALIDO", "HTTP_401"]);
const ROLES: Record<Usuario["rol"], string> = { PRODUCTOR: "Agricultor", ADMIN: "Administrador" };

export function inicialesDe(nombreCompleto: string): string {
  const partes = nombreCompleto.trim().split(/\s+/).filter(Boolean);
  return ((partes[0]?.[0] ?? "") + (partes.length > 1 ? partes[partes.length - 1][0] : "")).toUpperCase();
}

async function cargarUsuario(): Promise<UsuarioSesion> {
  const perfil = await obtenerMiPerfil();
  // El nombre del municipio es solo para mostrar: si territorio falla, se usa el código
  const municipio = await listarMunicipios(perfil.departamento_residencia)
    .then((lista) => lista.find((m) => m.codigo === perfil.municipio_residencia)?.nombre)
    .catch(() => undefined);
  return {
    id: perfil.id,
    nombre: perfil.nombre_completo.split(/\s+/)[0],
    nombreCompleto: perfil.nombre_completo,
    iniciales: inicialesDe(perfil.nombre_completo),
    rol: ROLES[perfil.rol] ?? perfil.rol,
    municipio: municipio ?? perfil.municipio_residencia,
  };
}

export function useSesionRequerida() {
  const router = useRouter();
  const [usuario, setUsuario] = useState<UsuarioSesion | null>(null);
  const [error, setError] = useState<ErrorApi | null>(null);
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    if (!haySesion()) {
      router.replace("/ingresar");
      return;
    }
    let cancelado = false;
    setError(null);
    cargarUsuario()
      .then((u) => !cancelado && setUsuario(u))
      .catch((err) => {
        if (cancelado) return;
        const e = aErrorApi(err);
        if (SESION_TERMINADA.has(e.codigo)) router.replace("/ingresar");
        else setError(e);
      });
    return () => {
      cancelado = true;
    };
  }, [router, intento]);

  const reintentar = useCallback(() => setIntento((n) => n + 1), []);
  return { usuario, error, reintentar };
}
