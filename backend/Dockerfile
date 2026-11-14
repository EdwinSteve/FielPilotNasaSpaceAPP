# Stage 1: BUILD
FROM maven:3.9-eclipse-temurin-21 AS builder

ARG APP_MODULE=infrastructure

WORKDIR /build
COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -pl ${APP_MODULE} -am clean package -DskipTests

RUN set -eux; \
    cp ${APP_MODULE}/target/backend-${APP_MODULE}-*.jar /tmp/application.jar; \
    java -Djarmode=tools -jar /tmp/application.jar extract --layers --destination /tmp/extracted; \
    for layer in dependencies spring-boot-loader snapshot-dependencies application; do \
      mkdir -p "/tmp/extracted/$layer"; \
    done

# Stage 2: RUNTIME
FROM eclipse-temurin:21-jre-jammy AS runtime

LABEL org.opencontainers.image.title="Surco-backend"
LABEL org.opencontainers.image.description="Spring Boot Hexagonal Modular Application"

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 spring \
    && useradd --system --uid 10001 --gid spring --no-create-home \
       --shell /usr/sbin/nologin spring

WORKDIR /application

COPY --from=builder /tmp/extracted/dependencies/ ./
COPY --from=builder /tmp/extracted/spring-boot-loader/ ./
COPY --from=builder /tmp/extracted/snapshot-dependencies/ ./
COPY --from=builder /tmp/extracted/application/ ./

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

USER 10001:10001

ENTRYPOINT ["java", "-jar", "application.jar"]