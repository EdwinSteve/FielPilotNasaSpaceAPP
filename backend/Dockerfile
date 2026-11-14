# Stage 1: BUILD
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

COPY pom.xml .

RUN mvn -B dependency:go-offline

COPY src ./src

RUN mvn -B clean package \
    -DskipTests \
    -Dcheckstyle.skip=true \
    -Dspotbugs.skip=true


RUN set -eux; \
    JAR_FILE="$(find target -maxdepth 1 -type f -name '*.jar' \
      ! -name '*-sources.jar' \
      ! -name '*-javadoc.jar' | head -n 1)"; \
    test -n "$JAR_FILE"; \
    cp "$JAR_FILE" /app.jar

# Stage 2: RUNTIME
FROM eclipse-temurin:21-jre-jammy AS runtime

LABEL org.opencontainers.image.title="Surco-backend"
LABEL org.opencontainers.image.description="Spring Boot Hexagonal Modular Application"

WORKDIR /app

RUN groupadd --system spring \
    && useradd --system \
       --gid spring \
       --home-dir /app \
       --shell /usr/sbin/nologin \
       spring

COPY --from=builder --chown=spring:spring /app.jar ./app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
               -XX:+UseContainerSupport \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

USER spring:spring

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
