FROM docker.io/library/node:18-bullseye-slim AS node-runtime

FROM docker.io/library/eclipse-temurin:17-jre
COPY --from=node-runtime /usr/local/ /usr/local/

WORKDIR /app
ARG JAR_PATH
COPY ${JAR_PATH} app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
