# Frontend SURCO (Next.js)

```
src/
├── app/            rutas (páginas delgadas)
│   ├── ingresar · registro · inicio · perfil · alertas
│   └── lotes/[loteId]/ diagnostico · rotacion
├── modulos/        uno por módulo del backend
│   └── <modulo>/   componentes/ · hooks/ · api.ts · tipos.ts · index.ts
└── compartido/     api/ (http, sesión, mocks) · ui/ · mapa/ · graficas/ · lib/
```

- Una página solo importa desde `@/modulos/<modulo>` (su `index.ts`) y `@/compartido`.
- `tipos.ts` refleja los schemas del backend; si el backend cambia, se cambia ahí primero.
- Con `NEXT_PUBLIC_USAR_MOCKS=1` las pantallas usan los datos de `compartido/api/mocks/` y se puede trabajar sin backend.
- Textos al agricultor: de **usted**, sencillos; el nombre de la fuente NASA va en un chip aparte.

```bash
cp .env.example .env.local
npm install
npm run dev     # http://localhost:3000
npm test
```
