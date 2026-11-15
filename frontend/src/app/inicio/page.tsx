// Ruta /inicio: página delgada que compone componentes de src/modulos.
// Exige sesión (InicioConSesion manda a /ingresar si no la hay) y pide los datos en el navegador,
// porque la API necesita el token de la sesión.
import { InicioConSesion } from "@/modulos/inicio";

export default function Page() {
  return <InicioConSesion />;
}
