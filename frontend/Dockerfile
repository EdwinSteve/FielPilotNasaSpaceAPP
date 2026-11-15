# Imagen del frontend (Next.js en modo standalone).
#   docker build --build-arg BACKEND_URL=http://surco-api:8080 -t surco-web frontend/
#   docker run -p 3000:3000 surco-web

# --- Dependencias: npm ci con el package-lock exacto ---
FROM node:26-alpine AS dependencias
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts

# --- Compilación ---
FROM node:26-alpine AS compilacion
WORKDIR /app
# Next.js fija el destino del proxy /api/v1 al compilar
ARG BACKEND_URL=http://surco-api:8080
ENV BACKEND_URL=${BACKEND_URL} NEXT_TELEMETRY_DISABLED=1
COPY --from=dependencias /app/node_modules node_modules
COPY . .
RUN npm run build

# --- Ejecución: solo el servidor standalone y los archivos estáticos, sin privilegios ---
FROM node:26-alpine
WORKDIR /app
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 PORT=3000 HOSTNAME=0.0.0.0
COPY --from=compilacion --chown=node:node /app/.next/standalone ./
COPY --from=compilacion --chown=node:node /app/.next/static .next/static
COPY --from=compilacion --chown=node:node /app/public public
# Numérico (uid del usuario "node" de la imagen): así Kubernetes puede verificar runAsNonRoot
USER 1000
EXPOSE 3000
CMD ["node", "server.js"]
