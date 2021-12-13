# java-backend with oracle-jdk8(alpine)
# Author: InCar
# Version: 1.0
# Build:
#   1. docker build --build-arg APP_NAME=java-backed \
#        --build-arg APP_VERSION=1.0 \
#        -t s1:5000/harbor_project/java-backed:1.0 .
# Testing:
#   1. docker run --rm -it --name backed -p 8080:8080 s1:5000/harbor_project/java-backend:1.0
#   2. docker exec -it backend ash
FROM s1:5000/library/oracle-jdk8:latest

MAINTAINER InCar

ARG APP_NAME=java-backed
ARG APP_VERSION=1.0

ENV APP_NAME=${APP_NAME} \
  APP_VERSION=${APP_VERSION}

EXPOSE 4000

WORKDIR /app

ADD build/libs/${APP_NAME}-${APP_VERSION}.jar /app/deploy.jar

ENTRYPOINT ["java", "-jar", "deploy.jar", "-Dsun.net.http.allowRestrictedHeaders=true"]