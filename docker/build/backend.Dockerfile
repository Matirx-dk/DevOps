FROM harbor.zoudekang.cloud/dockerhub-proxy/library/node:18.20.8-bullseye AS node-runtime

FROM harbor.zoudekang.cloud/dockerhub-proxy/library/eclipse-temurin:17-jre
COPY --from=node-runtime /usr/local/bin/node /usr/local/bin/node
COPY --from=node-runtime /usr/local/bin/npm /usr/local/bin/npm
COPY --from=node-runtime /usr/local/bin/npx /usr/local/bin/npx
COPY --from=node-runtime /usr/local/lib/node_modules /usr/local/lib/node_modules

RUN ln -sf /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
    && ln -sf /usr/local/lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx

WORKDIR /app
ARG JAR_PATH=aidevops-auth/target/aidevops-auth.jar
COPY ${JAR_PATH} app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
