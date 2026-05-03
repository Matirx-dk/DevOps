FROM nginx:1.25-alpine
COPY docker/nginx/conf/nginx.conf /etc/nginx/nginx.conf
COPY aidevops-ui/dist /home/aidevops/projects/aidevops-ui
EXPOSE 80
