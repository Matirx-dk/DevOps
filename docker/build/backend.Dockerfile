FROM 192.168.1.104/dockerhub-proxy/library/maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /src
COPY . .
RUN mvn -T 1C -DskipTests clean package

FROM 192.168.1.104/dockerhub-proxy/library/eclipse-temurin:17-jre
WORKDIR /app
ARG JAR_PATH
COPY --from=builder /src/${JAR_PATH} app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
