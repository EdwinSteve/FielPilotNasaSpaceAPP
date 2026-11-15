# Despliegue en Render

Field Shift se despliega en [Render](https://render.com) con un **Blueprint**: el archivo
[render.yaml](../render.yaml) de la raíz crea la base de datos y los tres servicios de una vez.

| Recurso de Render | Qué es | Imagen |
|---|---|---|
| `field-shift-db` | PostgreSQL 16 | (administrada por Render) |
| `field-shift-planificador` | Modelo estacional (Python) | [ml/planificador/Dockerfile](../ml/planificador/Dockerfile), contexto: raíz del repo |
| `field-shift-api` | API Spring Boot | [backend/Dockerfile](../backend/Dockerfile) |
| `field-shift-web` | Frontend Next.js: **la URL que se comparte** | [frontend/Dockerfile](../frontend/Dockerfile) |

```
navegador → field-shift-web ──/api/v1──→ field-shift-api ──→ field-shift-planificador
                                              └──→ field-shift-db
```

> Si Render dice `failed to read dockerfile: open Dockerfile: no such file or directory`, se creó un
> servicio suelto ("New > Web Service") que busca un Dockerfile en la raíz. Bórrelo y use el Blueprint.

## 1. Antes de empezar

Genere el secreto de los JWT (64 caracteres o más) y guárdelo:

```bash
openssl rand -base64 48
```

## 2. Crear el Blueprint

1. En Render: **New > Blueprint**, conecte el repositorio `EdwinSteve/FielPilotNasaSpaceAPP` y la rama `main`.
2. Render lee `render.yaml` y pide las variables marcadas `sync: false`:

   | Variable | Servicio | Valor |
   |---|---|---|
   | `JWT_SECRETO` | field-shift-api | El secreto del paso 1 |
   | `PLANIFICADOR_URL` | field-shift-api | `https://field-shift-planificador.onrender.com` |
   | `BACKEND_URL` | field-shift-web | `https://field-shift-api.onrender.com` |

   Las URL públicas son `https://<nombre-del-servicio>.onrender.com`. Si el nombre ya estaba tomado en
   Render, la URL lleva un sufijo (por ejemplo `field-shift-api-x1y2.onrender.com`). Por eso, si al
   terminar alguna URL no coincide, siga el paso 4.
3. **Apply**. La primera construcción tarda unos 10–15 minutos: el backend compila con Maven y el frontend
   con Next.js.

## 3. Comprobar

| Qué | URL | Respuesta esperada |
|---|---|---|
| Planificador | `https://field-shift-planificador.onrender.com/salud` | 200 |
| API | `https://field-shift-api.onrender.com/actuator/health` | `{"status":"UP"}` |
| App | `https://field-shift-web.onrender.com` | Pantalla de bienvenida |

Luego regístrese, dibuje un terreno y abra **Cultivos y rotación** y **Alertas IDEAM**. La primera
recomendación de una zona tarda 1–3 minutos (el planificador baja NASA POWER y SoilGrids).

## 4. Si una URL quedó distinta

- **API distinta** (`BACKEND_URL`): en `field-shift-web > Environment` corrija `BACKEND_URL` y luego
  **Manual Deploy > Clear build cache & deploy`**. Next.js fija esa URL al compilar, así que reiniciar no basta.
- **Planificador distinto** (`PLANIFICADOR_URL`): en `field-shift-api > Environment` corríjala y guarde
  (Render reinicia la API solo).
- **App distinta**: actualice `CORS_ORIGENES` en `field-shift-api`.

## Límites del plan gratuito

- **Se duermen** tras 15 minutos sin uso. Al volver, la primera visita tarda cerca de 1 minuto por
  servicio (la API de Spring Boot es la más lenta). Antes de una demo, abra las tres URL del paso 3.
- **512 MB de memoria** por servicio. Si la API o el planificador se reinician con "Out of memory",
  páselos a **Starter**.
- **La base gratuita vence a los 30 días.** Para conservar los datos, pase `field-shift-db` a un plan pago.
- **Sin disco:** la caché del planificador (NASA POWER, SoilGrids) se pierde en cada reinicio, así que la
  primera consulta de cada zona vuelve a tardar.

## Con planes de pago: red privada

Los servicios free no reciben tráfico por la red privada de Render, por eso aquí todo va por URL
públicas. Con planes de pago conviene que la API y el planificador sean privados:

1. En `render.yaml`, cambie el planificador (y si quiere la API) a `type: pserv` con `plan: starter`.
2. Reemplace las variables `sync: false` por referencias a la red privada:

   ```yaml
   - key: PLANIFICADOR_URL        # en field-shift-api
     fromService: { type: pserv, name: field-shift-planificador, property: hostport }
   - key: BACKEND_URL             # en field-shift-web
     fromService: { type: web, name: field-shift-api, property: hostport }
   ```

   `hostport` llega sin `http://`: `next.config.ts` y el cliente del planificador lo aceptan así.
3. Para conservar la caché del planificador, agréguele un disco (`disk: { name: datos, mountPath: /datos, sizeGB: 1 }`).
