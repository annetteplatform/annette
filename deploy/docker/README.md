# Deploy

To deploy prerequisites for development purposes run the following commands:

```bash
cd deploy/docker
./deploy.sh
```

This brings up Postgres, Cassandra 3.11, OpenSearch 2.8, MinIO, Keycloak, Camunda, Traefik and the console
frontend (backend services run from sbt in dev mode — see below).

## Dev mode (per-process, no `sbt runAll`)

Since the Pekko migration there is no Lagom `runAll`: every microservice is its own process. From the repo root:

```bash
./run-local.sh                 # tmux session "annette-dev": 9 services + api-gateway
./run-local.sh cms api-gateway # subset only
tmux attach -t annette-dev
```

The gateway binds `:9000`; services bind their gRPC ports (8510–8518) per the port table in `AGENTS.md`.
Seed demo data once the stack is healthy:

```bash
./run-ignition.sh
```

## Full docker-compose stack

`deploy.sh` also builds/pulls the backend images and starts them behind Traefik when the `all` profile is
selected (`BACKEND_VERSION` in the script selects the image tag).
