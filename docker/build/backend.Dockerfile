FROM docker.io/library/eclipse-temurin:17-jre
WORKDIR /app
ARG JAR_PATH
COPY ${JAR_PATH} app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
