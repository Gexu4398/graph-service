#!/bin/bash

docker pull maven:3.9.9-eclipse-temurin-21

TAG=`date +%Y%m%d%H%M%S`

docker run -it --rm --name build -v $(pwd):/usr/src/mymaven -v ~/.m2:/root/.m2 -w /usr/src/mymaven maven:3.9.9-eclipse-temurin-21 mvn -DskipTests=true clean install
docker build . --build-arg JAR_FILE=./biz-service/target/biz-service.jar -t graphservice/bizservice:$TAG
docker build . --build-arg JAR_FILE=./biz-batch-service/target/biz-batch-service.jar -t graphservice/bizbatchservice:$TAG

mkdir -p deploy/images

rm ./deploy/images/graphservice_bizservice_*
rm ./deploy/images/graphservice_bizbatchservice_*

docker save -o ./deploy/images/graphservice_bizservice_$TAG.tar graphservice/bizservice:$TAG
docker save -o ./deploy/images/graphservice_bizbatchservice_$TAG.tar graphservice/bizbatchservice:$TAG