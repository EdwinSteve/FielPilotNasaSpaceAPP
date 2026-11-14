import path from "node:path";
import type { NextConfig } from "next";

// En desarrollo la UI llama a /api/v1 en su mismo origen y Next.js lo reenvía a FastAPI:
// así no hace falta CORS. Por defecto, la API de Spring Boot en :8080; para otra, define BACKEND_URL.
const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

// Raíz fija del frontend: sin esto, si hay otro package-lock.json en una carpeta superior
// (por ejemplo en el home), Next.js toma esa carpeta como raíz del proyecto.
const RAIZ = path.resolve(__dirname);

const nextConfig: NextConfig = {
  outputFileTracingRoot: RAIZ,
  turbopack: { root: RAIZ },
  // El botón flotante de herramientas de Next tapaba los campos de los formularios en celular
  devIndicators: false,
  async rewrites() {
    return [{ source: "/api/v1/:ruta*", destination: `${BACKEND_URL}/api/v1/:ruta*` }];
  },
};

export default nextConfig;
