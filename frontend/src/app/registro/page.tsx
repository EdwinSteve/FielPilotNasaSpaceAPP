"use client";

import { useRouter } from "next/navigation";
import { FormularioRegistro } from "@/modulos/identidad";

// Ruta /registro: creación de la cuenta del productor. Después de crearla, registra su terreno en el mapa.
export default function Page() {
  const router = useRouter();
  return <FormularioRegistro alRegistrarse={() => setTimeout(() => router.push("/lotes/nuevo?bienvenida=1"), 1200)} />;
}
