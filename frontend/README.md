# Frontend SURCO (Next.js)

```
src/
├── app/            rutas (páginas delgadas)
│   ├── ingresar · registro · inicio · perfil · alertas
│   └── lotes/[loteId]/ diagnostico · rotacion
├── modulos/        uno por módulo del backend
│   └── <modulo>/   componentes/ · hooks/ · api.ts · tipos.ts · index.ts
└── compartido/     api/ (http, sesión) · ui/ · mapa/ · graficas/ · lib/
```

- Una página solo importa desde `@/modulos/<modulo>` (su `index.ts`) y `@/compartido`.
- `tipos.ts` refleja los DTOs del backend (JSON en snake_case); si el backend cambia, se cambia ahí primero.
- Textos al agricultor: de **usted**, sencillos; el nombre de la fuente NASA va en un chip aparte.

## Conexión con el backend

No hay datos simulados: todo va contra la API de Spring Boot (`backend/`). En desarrollo, Next.js reenvía `/api/v1/*` a `BACKEND_URL` (por defecto `http://localhost:8080`, ver `next.config.ts`), así el navegador llama al mismo origen y no hace falta CORS.

[`compartido/api/http.ts`](src/compartido/api/http.ts) maneja la sesión:
- Con `{ autenticada: true }` agrega `Authorization: Bearer <access_token>`.
- Si la API responde 401, renueva la sesión con `POST /auth/refresh` (una sola vez, aunque varias peticiones fallen a la vez) y reintenta. Si no puede, borra la sesión.
- Traduce los errores `{code, message, details}` al `ErrorApi` que usa la UI, con cada error de campo junto a su campo.

```ts
// Llamar una ruta protegida de un módulo nuevo
export async function misFincas(): Promise<Finca[]> {
  return peticion<Finca[]>("GET", "/fincas", { autenticada: true });
}
```

## Pantallas listas

| Ruta | Pantalla | Módulo |
|---|---|---|
| `/` | Bienvenida: qué hace SURCO, crear cuenta o ingresar | `identidad` |
| `/registro` | Cuenta del productor: nombre, celular (+57), correo opcional, contraseña, departamento y municipio (toda Colombia) y autorización de datos (Ley 1581) | `identidad` + `territorio` |
| `/ingresar` | Ingreso con celular y contraseña | `identidad` |
| `/politica-de-datos` | Texto de la política vigente | `identidad` |
| `/inicio` | Provisional: saluda con el nombre que devuelve la ruta protegida `GET /usuarios/me`; sin sesión manda a `/ingresar` | `inicio` |

Los errores del backend llegan al campo que hay que corregir: celular ya registrado (con enlace a "Ingresar aquí"), correo duplicado, credenciales incorrectas, cuenta bloqueada, política desactualizada (se recarga y se pide aceptarla otra vez).

## Correr

Primero el backend (ver [backend/README.md](../backend/README.md)), luego:

```bash
cp .env.example .env.local
npm install
npm run dev     # http://localhost:3000
npm test
```
