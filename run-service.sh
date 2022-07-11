#!/usr/bin/env bash

export INDEX_PREFIX="dev-"
export INDEXING_ALLOW_INSECURE="true"
export KEYSPACE_PREFIX="dev_"
export POSTGRES_PREFIX="dev_"
export CMS_STORAGE_BUCKET_PREFIX="dev-"
export MINIO_PREFIX="dev_"
export KEYCLOAK_CLIENT="annette-console"
export KEYCLOAK_REALM="AnnetteDemo"
#export KEYCLOAK_URL="https://kc.apps.cloud4.ambergate.ru/auth"
export KEYCLOAK_URL="http://localhost:3080/auth"
echo "$1/run"
sbt -Dsbt.supershell=false -Dconfig.resource="application.dev.conf" "$1/run"
