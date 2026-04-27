# Backend 微服务镜像
# 用法: docker build -f docker/build/backend.Dockerfile --build-arg JAR_PATH=aidevops-gateway/target/aidevops-gateway.jar --build-arg EXPOSE=8080 -t aidevops-gateway .
ARG JDK_IMAGE=harbor.zoudekang.cloud/dockerhub-proxy/library/eclipse-temurin:17-jre
FROM ${JDK_IMAGE}

ARG JAR_PATH=aidevops-gateway/target/aidevops-gateway.jar
ARG EXPOSE=8080

WORKDIR /home/aidevops

# 复制 JAR（路径由 build arg 指定）
COPY ${JAR_PATH} app.jar

EXPOSE ${EXPOSE}

ENTRYPOINT ["java", "-jar", "/home/aidevops/app.jar"]
