"use client";

import { useRouter } from "next/navigation";
import { FormularioRegistro } from "@/modulos/identidad";

// Ruta /registro: creación de la cuenta del productor.
export default function Page() {
  const router = useRouter();
  return <FormularioRegistro alRegistrarse={() => setTimeout(() => router.push("/inicio"), 1200)} />;
}
