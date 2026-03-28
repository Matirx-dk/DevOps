FROM node:18-alpine AS builder
WORKDIR /src
COPY ruoyi-ui ./
RUN npm config set registry https://registry.npmmirror.com \
 && npm install \
 && npm run build:prod

FROM nginx:1.27-alpine
COPY docker/nginx/conf/nginx.conf /etc/nginx/nginx.conf
COPY --from=builder /src/dist /home/ruoyi/projects/ruoyi-ui
EXPOSE 80
