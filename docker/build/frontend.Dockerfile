FROM harbor.zoudekang.cloud/dockerhub-proxy/library/node:18-alpine AS builder
WORKDIR /src
COPY aidevops-ui ./
RUN npm config set registry https://registry.npmmirror.com \
 && npm install \
 && npm run build:prod

FROM harbor.zoudekang.cloud/dockerhub-proxy/library/nginx:1.27-alpine
COPY docker/nginx/conf/nginx.conf /etc/nginx/nginx.conf
COPY --from=builder /src/dist /home/aidevops/projects/aidevops-ui
EXPOSE 80
