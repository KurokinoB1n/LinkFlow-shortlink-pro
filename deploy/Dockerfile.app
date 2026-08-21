
FROM maven:3.9-eclipse-temurin-17 AS builder
ARG MODULE
WORKDIR /build
COPY deploy/settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -s /root/.m2/settings.xml dependency:go-offline -B || true
COPY . .
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -s /root/.m2/settings.xml clean package -pl ${MODULE} -am -DskipTests -B \
    -Dmaven.wagon.http.pool=false \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=25

FROM eclipse-temurin:17-jre-alpine
ARG MODULE
WORKDIR /app
COPY --from=builder /build/${MODULE}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]