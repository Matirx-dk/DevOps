# Backend 微服务镜像 - 多阶段构建
# Stage 1: Maven 打包（确保所有资源文件正确打入 JAR）
# Stage 2: JRE 运行（只含运行时）
#
# 用法: docker build -f docker/build/backend.Dockerfile \
#   --build-arg MODULE_DIR=aidevops-gateway \
#   --build-arg EXPOSE=8080 \
#   -t aidevops-gateway .

# ===== Stage 1: Build =====
FROM harbor.zoudekang.cloud/dockerhub-proxy/library/eclipse-temurin:17-jdk AS builder

ARG MODULE_DIR=aidevops-gateway
ARG EXPOSE=8080

WORKDIR /build

# 复制模块源码（Maven 和源码一起 COPY，确保 resources/mapper 等文件被正确打包）
COPY pom.xml ${MODULE_DIR}/pom.xml ./
COPY aidevops-common ${MODULE_DIR}/../aidevops-common
COPY aidevops-modules/${MODULE_DIR} ${MODULE_DIR}/

# Maven 打包（自动包含 src/main/resources 下所有文件）
RUN mvn -f ${MODULE_DIR}/pom.xml clean package -DskipTests

# ===== Stage 2: Run =====
FROM harbor.zoudekang.cloud/dockerhub-proxy/library/eclipse-temurin:17-jre

ARG EXPOSE=8080

WORKDIR /home/aidevops

# 从 Stage 1 复制打包好的 JAR
COPY --from=builder /build/${MODULE_DIR}/target/*.jar app.jar

EXPOSE ${EXPOSE}

ENTRYPOINT ["java", "-jar", "/home/aidevops/app.jar"]
