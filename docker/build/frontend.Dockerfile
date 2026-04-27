# 前端镜像
# 用法: docker build -f docker/build/frontend.Dockerfile --build-arg DIST_PATH=aidevops-ui/dist -t aidevops-ui .
ARG NGINX_IMAGE=harbor.zoudekang.cloud/dockerhub-proxy/library/nginx:1.25-alpine
FROM ${NGINX_IMAGE}

ARG DIST_PATH=aidevops-ui/dist

COPY ${DIST_PATH} /home/aidevops/projects/aidevops-ui

EXPOSE 80
