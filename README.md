# onlydoc

[![java](https://img.shields.io/badge/java-1.8-brightgreen.svg?style=flat&logo=java)](https://www.oracle.com/java/technologies/javase-downloads.html)
[![spring](https://img.shields.io/badge/springboot-2.3.2-brightgreen.svg?style=flat&logo=spring)](https://docs.spring.io/spring-boot/docs/2.3.x-SNAPSHOT/reference/htmlsingle)
[![gradle](https://img.shields.io/badge/gradle-7.2-brightgreen.svg?style=flat&logo=gradle)](https://docs.gradle.org/7.2/userguide/installation.html)
[![build](https://github.com/InCar/onlydoc/workflows/build/badge.svg)](https://github.com/InCar/onlydoc/actions)
[![release](https://img.shields.io/badge/release-1.0.2-blue.svg)](https://github.com/InCar/onlydoc/releases)

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
#    volumes:
#      - ./onlyoffice/logs:/var/log/onlyoffice
#      - ./onlyoffice/data:/var/www/onlyoffice/Data
#      - ./onlyoffice/lib:/var/lib/onlyoffice
#      - ./onlyoffice/db:/var/lib/postgresql
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
  &fileUrl=http://10.0.11.25:9333/1,01fc39db1909
  &thirdUri=true
  &lang=zh
  &displayName=张三
```

|No.|Name|Type|Remark|
|:---:|:---:|:----|-----|
|1|type|`string`|*类型：desktop-桌面（默认），mobile-手机，embedded-嵌入式*|
|2|action|`string`|*动作：view-预览*|
|3|fileName|`string`|*文件名称*|
|4|fileKey|`string`|*文件在OnlyOffice缓存Key，必需唯一，默认同`fileName`*|
|5|fileUrl|`string`|*文件外部URL，当`thirdUri=true`时有效*|
|6|thirdUri|`boolean`|*使用文件外部URL*|
|7|lang|`string`|*语言：en-英文（默认），zh-中文*|
|8|displayName|`string`|*使用人，没有显示“匿名”*|

### 5.2 Testing

|No.|File|Testing|Remark|
|:---:|:---:|:---:|-----|
|1|[test.xlsx](http://10.0.11.25:9333/1,01fc39db1909)|[Excel](http://127.0.0.1:18080/onlydoc/editor?type=desktop&action=view&fileName=test.xlsx&fileKey=test2xlsx&fileUrl=http://10.0.11.25:9333/1,01fc39db1909&thirdUri=true&lang=zh&displayName=张三)|*InCar Only*|
|2|[test.docx](http://10.0.11.25:9333/6,01fe0f8fd5a4)|[Word](http://127.0.0.1:18080/onlydoc/editor?type=desktop&action=view&fileName=test.docx&fileKey=test2docx&fileUrl=http://10.0.11.25:9333/6,01fe0f8fd5a4&thirdUri=true&lang=zh&displayName=张三)||
|3|[test.pptx](http://10.0.11.25:9333/6,01ff978bc1c3)|[PowerPoint](http://127.0.0.1:18080/onlydoc/editor?type=desktop&action=view&fileName=test.pptx&fileKey=test2pptx&fileUrl=http://10.0.11.25:9333/6,01ff978bc1c3&thirdUri=true&lang=zh&displayName=张三)||

### 6 About File Access Limit

&emsp;&emsp;**It is recommended to separate view and access, adding your token to `fileUrl`.**
