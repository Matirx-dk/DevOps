FROM harbor.zoudekang.cloud/dockerhub-proxy/library/nginx:1.27-alpine
COPY docker/nginx/conf/nginx.conf /etc/nginx/nginx.conf
COPY aidevops-ui/dist /home/aidevops/projects/aidevops-ui
EXPOSE 80
