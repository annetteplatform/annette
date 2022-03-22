#!/usr/bin/env bash

oc create secret generic elastic-secret \
    --from-literal=INDEXING_USERNAME=admin \
    --from-literal=INDEXING_PASSWORD=$SECRET_PASSWORD$

oc create secret generic cassandra-secret \
    --from-literal=CASSANDRA_USERNAME=cassandra \
    --from-literal=CASSANDRA_PASSWORD=$SECRET_PASSWORD$

oc create secret generic play-secret \
    --from-literal=SECRET_KEY=$SECRET$

oc create secret generic minio-secret \
    --from-literal=MINIO_ACCESS_KEY=$ACCESS_KEY$ \
    --from-literal=MINIO_SECRET_KEY="$SECRET_KEY$"

oc create secret generic bpm-repository-secret \
    --from-literal=POSTGRES_USERNAME="bpm_repository" \
    --from-literal=POSTGRES_PASSWORD=$SECRET_PASSWORD$

oc create secret generic camunda-client-secret \
    --from-literal=CAMUNDA_LOGIN=camunda \
    --from-literal=CAMUNDA_PASSWORD=$SECRET_PASSWORD$

oc create secret generic camunda-secret \
    --from-literal=DB_USERNAME="annette_camunda" \
    --from-literal=DB_PASSWORD=$SECRET_PASSWORD$



