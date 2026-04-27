# Backend 微服务镜像 - 多阶段构建
# Stage 1: Maven 构建（maven:3.9-eclipse-temurin-17 自带 JDK 17 + Maven 3.9）
# Stage 2: JRE 运行（只有 JRE，体积小）
#
# 用法: docker build -f docker/build/backend.Dockerfile \
#   --build-arg MODULE_DIR=aidevops-gateway \
#   --build-arg EXPOSE=8080 \
#   -t aidevops-gateway .

# ===== Stage 1: Maven Build =====
FROM harbor.zoudekang.cloud/dockerhub-proxy/library/maven:3.9-eclipse-temurin-17 AS builder

ARG MODULE_DIR=aidevops-gateway
ARG EXPOSE=8080
ARG MAVEN_OPTS="-Xmx512m"

WORKDIR /build

# 先复制 pom.xml 下载依赖（利用 Docker 层缓存）
COPY pom.xml ${MODULE_DIR}/pom.xml
COPY aidevops-common/pom.xml ${MODULE_DIR}/../aidevops-common/pom.xml
COPY aidevops-modules/${MODULE_DIR}/pom.xml ${MODULE_DIR}/pom.xml
RUN mvn -f ${MODULE_DIR}/pom.xml dependency:go-offline -B

# 再复制源码，Maven 打包（src/main/resources 会自动打入 JAR）
COPY aidevops-common ${MODULE_DIR}/../aidevops-common
COPY aidevops-modules/${MODULE_DIR} ${MODULE_DIR}/

RUN mvn -f ${MODULE_DIR}/pom.xml clean package -DskipTests

# ===== Stage 2: JRE Run =====
FROM harbor.zoudekang.cloud/dockerhub-proxy/library/eclipse-temurin:17-jre

ARG EXPOSE=8080

WORKDIR /home/aidevops

# 从 Stage 1 复制打包好的 JAR
COPY --from=builder /build/${MODULE_DIR}/target/*.jar app.jar

EXPOSE ${EXPOSE}

ENTRYPOINT ["java", "-jar", "/home/aidevops/app.jar"]
