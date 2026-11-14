// Llamadas HTTP del módulo territorio contra la API de Spring Boot. Públicas: el registro las usa sin sesión.
import { peticion } from "@/compartido/api/http";
import type { Departamento, Municipio } from "./tipos";

/** GET /territorio/departamentos: los 33 departamentos (incluye Bogotá D.C.), en orden alfabético. */
export async function listarDepartamentos(): Promise<Departamento[]> {
  return peticion<Departamento[]>("GET", "/territorio/departamentos");
}

/** GET /territorio/municipios?departamento=05: municipios del departamento, en orden alfabético. */
export async function listarMunicipios(departamento: string): Promise<Municipio[]> {
  return peticion<Municipio[]>("GET", `/territorio/municipios?departamento=${encodeURIComponent(departamento)}`);
}
