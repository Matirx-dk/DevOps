FROM docker.io/library/eclipse-temurin:17-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends nodejs npm \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
ARG JAR_PATH
COPY ${JAR_PATH} app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
