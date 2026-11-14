# Imagen de la API. El CI ya corrió las pruebas antes de construirla, por eso aquí se omiten.
#   docker build -t surco-api backend/
#   docker run -p 8080:8080 -e JWT_SECRETO=... -e DATABASE_URL=jdbc:postgresql://host:5432/surco surco-api

# --- Compilación ---
FROM eclipse-temurin:24-jdk AS compilacion
WORKDIR /app
# Primero solo lo que define las dependencias: Docker reutiliza esta capa mientras pom.xml no cambie
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp dependency:go-offline
COPY src src
RUN ./mvnw -B -ntp -DskipTests package && cp target/surco-api-*.jar app.jar

# --- Ejecución: solo el JRE y el jar, con un usuario sin privilegios ---
FROM eclipse-temurin:24-jre
RUN useradd --system --uid 10001 --no-create-home surco
WORKDIR /app
COPY --from=compilacion /app/app.jar app.jar
# Numérico: así Kubernetes puede verificar runAsNonRoot
USER 10001
EXPOSE 8080
# Usa hasta el 75 % de la memoria del contenedor para el heap
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
