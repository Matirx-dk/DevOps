FROM harbor.zoudekang.cloud/dockerhub-proxy/library/maven:3.9.9-eclipse-temurin-17

USER root

ARG KUBECTL_VERSION=v1.28.15
ARG NODE_MAJOR=18

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    git \
    bash \
    jq \
    unzip \
    gnupg \
 && curl -fsSL https://deb.nodesource.com/setup_${NODE_MAJOR}.x | bash - \
 && apt-get install -y --no-install-recommends nodejs \
 && curl -fsSL -o /usr/local/bin/kubectl https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl \
 && chmod +x /usr/local/bin/kubectl \
 && npm config set registry https://registry.npmmirror.com \
 && npm install -g npm@10 \
 && rm -rf /var/lib/apt/lists/*

USER 1000
WORKDIR /home/jenkins/agent

CMD ["cat"]
