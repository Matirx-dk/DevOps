FROM harbor.zoudekang.cloud/dockerhub-proxy/library/maven:3.9-eclipse-temurin-17

USER root

ARG KUBECTL_VERSION=v1.28.15
ARG NODE_MAJOR=18
ARG NPM_VERSION=10.9.0

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    git \
    bash \
    jq \
    unzip \
    gnupg \
    wget \
 && curl -fsSL "https://nodejs.org/dist/v${NODE_MAJOR}.20.0/node-v${NODE_MAJOR}.20.0-linux-x64.tar.xz" \
    | tar -xJ -C /usr/local --strip-components=1 \
 && curl -fsSL "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl" \
    -o /usr/local/bin/kubectl \
 && curl -fsSL "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl.sha256" \
    | tee /tmp/kubectl.sha256 \
    | awk '{print $1}' | xargs -I{} sh -c 'echo "{}  /usr/local/bin/kubectl" | sha256sum -c' \
 && chmod +x /usr/local/bin/kubectl \
 && npm config set registry https://registry.npmmirror.com \
 && npm install -g "npm@${NPM_VERSION}" \
 && rm -rf /var/lib/apt/lists/* /tmp/kubectl.sha256

USER 1000
WORKDIR /home/jenkins/agent

CMD ["cat"]
