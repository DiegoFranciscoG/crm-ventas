# syntax=docker/dockerfile:1
# ---- Etapa 1: compilación (los tests corren en CI con Testcontainers, no dentro de la imagen)
FROM maven:3.9.15-eclipse-temurin-26-alpine AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp package -DskipTests -Djacoco.skip=true \
    && java -Djarmode=tools -jar target/crm-ventas-api-1.0.0.jar extract --layers --launcher --destination extracted

# ---- Etapa 2: ejecución con JRE mínimo y usuario sin privilegios
FROM eclipse-temurin:25.0.4_7-jre-alpine
RUN addgroup -S app && adduser -S -G app -H -s /sbin/nologin app
WORKDIR /app
# Capas de Spring Boot: las dependencias cambian poco y se reutilizan entre builds.
COPY --from=build --chown=app:app /build/extracted/dependencies/ ./
COPY --from=build --chown=app:app /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /build/extracted/application/ ./
USER app
EXPOSE 8080
# Render free da 512 MB: el heap se limita al 75 % de la memoria del contenedor.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
