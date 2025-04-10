#!/bin/bash

docker pull maven:3.9.9-eclipse-temurin-21
docker pull postgres:alpine3.21
docker pull keycloak/keycloak:26.1.4
docker pull bitnami/zookeeper:3.9.3
docker pull bitnami/kafka:3.9.0
docker pull minio/minio:RELEASE.2025-02-28T09-55-16Z
docker pull minio/mc:RELEASE.2025-02-21T16-00-46Z
docker pull elasticsearch:8.15.3
docker pull redis:7.4.2-alpine
docker pull neo4j:5.26.2
docker pull eclipse-temurin:21-jre-alpine
docker pull nginx:alpine3.21

TAG=`date +%Y%m%d%H%M%S`

docker run -it --rm --name build -v $(pwd):/usr/src/mymaven -v ~/.m2:/root/.m2 -w /usr/src/mymaven maven:3.9.9-eclipse-temurin-21 mvn -DskipTests=true clean install
docker build . --build-arg JAR_FILE=./biz-service/target/biz-service.jar -t graphservice/bizservice:$TAG
docker build . --build-arg JAR_FILE=./biz-batch-service/target/biz-batch-service.jar -t graphservice/bizbatchservice:$TAG

mkdir -p deploy/images

rm ./deploy/images/graphservice_bizservice_*
rm ./deploy/images/graphservice_bizbatchservice_*

docker save -o ./deploy/images/graphservice_bizservice_$TAG.tar graphservice/bizservice:$TAG
docker save -o ./deploy/images/graphservice_bizbatchservice_$TAG.tar graphservice/bizbatchservice:$TAG

docker save -o ./deploy/images/postgres.tar postgres:alpine3.21
docker save -o ./deploy/images/keycloak.tar keycloak/keycloak:26.1.4
docker save -o ./deploy/images/bitnami_zookeeper.tar bitnami/zookeeper:3.9.3
docker save -o ./deploy/images/bitnami_kafka.tar bitnami/kafka:3.9.0
docker save -o ./deploy/images/minio.tar minio/minio:RELEASE.2025-02-28T09-55-16Z
docker save -o ./deploy/images/minio_mc.tar minio/mc:RELEASE.2025-02-21T16-00-46Z
docker save -o ./deploy/images/elasticsearch.tar elasticsearch:8.15.3
docker save -o ./deploy/images/redis.tar redis:7.4.2-alpine
docker save -o ./deploy/images/neo4j.tar neo4j:5.26.2
docker save -o ./deploy/images/nginx.tar nginx:alpine3.21