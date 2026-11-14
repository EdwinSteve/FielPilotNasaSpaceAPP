"use client";

import { useRouter } from "next/navigation";
import { FormularioIngreso } from "@/modulos/identidad";

// Ruta /ingresar: inicio de sesión con celular y contraseña.
export default function Page() {
  const router = useRouter();
  return <FormularioIngreso alIngresar={() => router.push("/inicio")} />;
}
