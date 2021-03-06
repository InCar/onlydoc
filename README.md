# onlydoc

[![jdk](https://img.shields.io/badge/jdk-1.8-success.svg?style=flat&logo=java)](https://www.oracle.com/java/technologies/javase-downloads.html)
[![spring boot](https://img.shields.io/badge/spring_boot-2.3.2-success.svg?style=flat&logo=spring)](https://docs.spring.io/spring-boot/docs/2.3.x-SNAPSHOT/reference/htmlsingle)
[![gradle](https://img.shields.io/badge/gradle-7.2-success.svg?style=flat&logo=gradle)](https://docs.gradle.org/7.2/userguide/installation.html)
[![build](https://github.com/InCar/onlydoc/workflows/build/badge.svg)](https://github.com/InCar/onlydoc/actions)
[![release](https://img.shields.io/badge/release-1.0.2-success.svg)](https://github.com/InCar/onlydoc/releases)

> OnlyOffice Integration.

## 1 Development

> [Java with Spring Boot](https://api.onlyoffice.com/editors/example/javaspring)

#### 1.1 application-dev.yml

```yaml
# Files settings
files:
  docservice:
    url:
      site: http://127.0.0.1:18080/onlyoffice/

# Logo settings
logo:
  url: https://www.incarcloud.com
```

#### 1.2 Build

```bash
./gradlew clean build
```

## 2. Docker image for `onlydoc:1.0.2`

### 2.1 Dockerfile

```dockerfile
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
```

### 2.2 Build Image

```bash
docker build --build-arg APP_NAME=onlydoc \
  --build-arg APP_VERSION=1.0.2 \
  -t onlydoc:1.0.2 .
```

## 3. Deployment

> [Overview of Docker Compose](https://docs.docker.com/compose/)

|No.|Service|Inter Port|Outer Port|Remark|
|:---:|:---:|:---:|:---:|----|
|1|*onlyoffice*|`80`|`8080`|*will proxy by nginx*|
|2|*onlydoc*|`4000`|`4000`||

```bash
# yaml
cat > docker-compose.yaml <<-'EOF'
version: "3"
services:
  onlyoffice:
    image: onlyoffice/documentserver:6.4.2
    container_name: onlyoffice_community_edition
    # ???????????????????????????
#    volumes:
#      - ./onlyoffice/logs:/var/log/onlyoffice
#      - ./onlyoffice/data:/var/www/onlyoffice/Data
#      - ./onlyoffice/lib:/var/lib/onlyoffice
#      - ./onlyoffice/db:/var/lib/postgresql
    # ???????????????????????????docker?????????????????????OnlyOffice?????????????????????
    # ??????????????????????????????IP?????????OnlyOffice????????????
#    extra_hosts:
#      - "project.domain.com:192.168.0.100"
    dns:
      - 8.8.8.8
    environment:
      - ONLYOFFICE_HTTPS_HSTS_ENABLED=false
    ports:
      - 8080:80
    restart: always
    deploy:
      restart_policy:
        condition: on-failure
  onlydoc:
    image: onlydoc:1.0.2
    container_name: onlydoc_integration_edition
    environment:
      - DOCUMENTSERVER_SITE=http://127.0.0.1:18080/onlyoffice/
    ports:
      - 4000:4000
    restart: always
    deploy:
      restart_policy:
        condition: on-failure
EOF

# start
docker-compose up -d
```

## 4 Nginx

### 4.1 localhost-18080.conf

```nginx
map $http_upgrade $connection_upgrade {
    default upgrade;
    '' close;
}

upstream onlydoc-servers {
    server 127.0.0.1:4000;
}

upstream onlyoffice-servers {
    server 127.0.0.1:8080;
}

server {
    listen       18080;
    server_name localhost;

    charset utf-8;

    # onlydoc
    location ^~ /onlydoc/ {
        proxy_pass http://onlydoc-servers/;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $http_host/onlydoc;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Real-PORT $remote_port;
    }

    # onlyoffice
    location ^~ /onlyoffice/ {
        proxy_pass http://onlyoffice-servers/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $http_host/onlyoffice;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Real-PORT $remote_port;
        proxy_connect_timeout 15s;
        proxy_read_timeout 120s;
        proxy_send_timeout 60s;
    }
    
    # add your project proxy here
}
```

### 4.2 Reload

```bash
# valid
/usr/local/nginx/sbin/nginx -t

# reload
/usr/local/nginx/sbin/nginx -s reload
```

## 5 Usage

### 5.1 Params

```text
http://127.0.0.1:18080/onlydoc/editor?type=desktop
  &action=view
  &fileName=test.xlsx
  &fileKey=test2xlsx
  &fileUrl=http://10.0.11.25:9081/1,0305f2988b
  &thirdUri=true
  &lang=zh
  &displayName=??????
```

|No.|Name|Type|Remark|
|:---:|:---:|:----|-----|
|1|type|`string`|*?????????desktop-?????????????????????mobile-?????????embedded-?????????*|
|2|action|`string`|*?????????view-??????*|
|3|fileName|`string`|*????????????*|
|4|fileKey|`string`|*?????????OnlyOffice??????Key???????????????????????????`fileName`*|
|5|fileUrl|`string`|*????????????URL??????`thirdUri=true`?????????*|
|6|thirdUri|`boolean`|*??????????????????URL*|
|7|lang|`string`|*?????????en-?????????????????????zh-??????*|
|8|displayName|`string`|*????????????????????????????????????*|

### 5.2 Testing

> *??????`fileUrl`??????????????????URL???????????????*

|No.|File|Testing|Remark|
|:---:|:---:|:---:|-----|
|1|[test.xlsx](http://10.0.11.25:9081/1,0305f2988b)|[Excel](http://127.0.0.1:18080/onlydoc/editor?type=desktop&action=view&fileName=test.xlsx&fileKey=test2xlsx&fileUrl=http%3A%2F%2F10.0.11.25%3A9081%2F1%2C0305f2988b&thirdUri=true&lang=zh&displayName=??????)|*InCar Only*|
|2|[test.docx](http://10.0.11.25:9081/2,028758913b)|[Word](http://127.0.0.1:18080/onlydoc/editor?type=desktop&action=view&fileName=test.docx&fileKey=test2docx&fileUrl=http%3A%2F%2F10.0.11.25%3A9081%2F2%2C028758913b&thirdUri=true&lang=zh&displayName=??????)||
|3|[test.pptx](http://10.0.11.25:9081/3,0f55954043)|[PowerPoint](http://127.0.0.1:18080/onlydoc/editor?type=desktop&action=view&fileName=test.pptx&fileKey=test2pptx&fileUrl=http%3A%2F%2F10.0.11.25%3A9081%2F3%2C0f55954043&thirdUri=true&lang=zh&displayName=??????)||

### 6 About File Access Limit

&emsp;&emsp;**It is recommended to separate view and access, adding your token to `fileUrl`.**
