import path from "node:path";
import type { NextConfig } from "next";

// La UI llama a /api/v1 en su mismo origen y Next.js lo reenvía a la API de Spring Boot: así no hace falta CORS.
// Por defecto la API está en :8080; para otra, defina BACKEND_URL. Ojo: Next.js fija este valor al compilar
// (next build), por eso la imagen Docker lo recibe como build-arg (en Render, la variable de entorno
// BACKEND_URL del servicio, ver render.yaml). Acepta "https://api.onrender.com" o "host:puerto" de la red
// privada (sin esquema: se asume http).
function urlBackend(valor = "http://localhost:8080"): string {
  const url = valor.trim().replace(/\/+$/, "");
  return /^https?:\/\//.test(url) ? url : `http://${url}`;
}
const BACKEND_URL = urlBackend(process.env.BACKEND_URL);

// Raíz fija del frontend: sin esto, si hay otro package-lock.json en una carpeta superior
// (por ejemplo en el home), Next.js toma esa carpeta como raíz del proyecto.
const RAIZ = path.resolve(__dirname);

const nextConfig: NextConfig = {
  // Genera .next/standalone: un servidor Node mínimo con solo las dependencias que usa (imagen Docker liviana)
  output: "standalone",
  outputFileTracingRoot: RAIZ,
  turbopack: { root: RAIZ },
  // El botón flotante de herramientas de Next tapaba los campos de los formularios en celular
  devIndicators: false,
  experimental: {
    // El proxy de /api/v1 corta a los 30 s por defecto ("socket hang up"), pero la primera recomendación de
    // una zona tarda 1-3 minutos (el planificador baja NASA POWER y SoilGrids; la API lo espera hasta 180 s,
    // surco.planificador.tiempo-maximo). Este valor debe ser mayor que ese.
    proxyTimeout: 240_000,
  },
  async rewrites() {
    return [{ source: "/api/v1/:ruta*", destination: `${BACKEND_URL}/api/v1/:ruta*` }];
  },
};

export default nextConfig;
