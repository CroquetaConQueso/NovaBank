ARG SERVICE_NAME=api-gateway
ARG SERVICE_PORT=8080

FROM maven:3.9-eclipse-temurin-17 AS builder
ARG SERVICE_NAME
WORKDIR /workspace

COPY pom.xml .
COPY novabank-events/pom.xml novabank-events/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-server/pom.xml auth-server/pom.xml
COPY cliente-service/pom.xml cliente-service/pom.xml
COPY cuenta-service/pom.xml cuenta-service/pom.xml
COPY operacion-service/pom.xml operacion-service/pom.xml
COPY documento-service/pom.xml documento-service/pom.xml
COPY comision-lambda/pom.xml comision-lambda/pom.xml
COPY exchange-rate-mock-service/pom.xml exchange-rate-mock-service/pom.xml
COPY notificacion-service/pom.xml notificacion-service/pom.xml

RUN mvn -B -pl ${SERVICE_NAME} -am dependency:go-offline -DskipTests

COPY . .
RUN mvn -B -pl ${SERVICE_NAME} -am package -DskipTests \
    && cp ${SERVICE_NAME}/target/*.jar /workspace/app.jar

FROM eclipse-temurin:17-jre
ARG SERVICE_PORT
WORKDIR /app

RUN addgroup --system novabank && adduser --system --ingroup novabank novabank
COPY --from=builder --chown=novabank:novabank /workspace/app.jar app.jar

USER novabank
EXPOSE ${SERVICE_PORT}
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
