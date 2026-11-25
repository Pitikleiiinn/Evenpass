# syntax=docker/dockerfile:1
# Imagen del microservicio eventos-service (puerto 8082)
# Buenas practicas aplicadas: build multi-etapa, cache de dependencias Maven, capas separadas del
# jar de Spring Boot, imagen final JRE-alpine (liviana), usuario sin privilegios y HEALTHCHECK.

# ---------- Etapa 1: compilacion ----------
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

# 1) Primero solo el pom: esta capa se reutiliza mientras no cambien las dependencias
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

# 2) Luego el codigo fuente; se genera el jar y se divide en capas (dependencias / aplicacion)
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/app.jar extract --layers --destination extracted

# ---------- Etapa 2: ejecucion ----------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S eventpass && adduser -S eventpass -G eventpass
WORKDIR /app

# Las capas que menos cambian van primero para aprovechar la cache al reconstruir
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER eventpass
EXPOSE 8082
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
