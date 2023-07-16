#!/usr/bin/env bash

export BACKEND_VERSION="0.5.0-SNAPSHOT"
docker run -it --network "annette_default" \
   -e JAVA_OPTS='-Dconfig.file=/opt/docker/conf/application.demo.conf' \
   annetteplatform/demo-ignition:$BACKEND_VERSION
