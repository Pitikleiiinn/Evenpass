# syntax=docker/dockerfile:1
# Ejecuta la funcion de emision en local (LocalRunner) consumiendo la cola ElasticMQ.
# En AWS NO se usa esta imagen: la funcion se despliega como Lambda (ver infra/cloudformation.yml).

FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S eventpass && adduser -S eventpass -G eventpass
WORKDIR /app
COPY --from=build /workspace/target/lambda-emision.jar ./lambda-emision.jar
USER eventpass
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-cp", "lambda-emision.jar", "cl.eventpass.lambda.LocalRunner"]
