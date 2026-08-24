# Docker deployment

Two deployments, driven by Docker Compose profiles and started with a single script:

| Mode   | Command               | What it starts                                                                                         |
|--------|-----------------------|--------------------------------------------------------------------------------------------------------|
| dev    | `./deploy.sh`         | Core infrastructure only: Postgres, Cassandra, OpenSearch, MinIO, Keycloak, Camunda. Backend services run from sbt (`./run-local.sh` at the repo root). |
| full   | `./deploy.sh full`    | The complete platform: infrastructure + Traefik + frontend + api-gateway + all 9 backend services + demo-ignition (seeds demo data automatically). |
| +tools | `… --tools`           | Add pgAdmin, OpenSearch Dashboards and Traefik to either mode, e.g. `./deploy.sh full --tools`.         |

Prerequisites: Docker with Compose v2 (`docker compose …`). The compose project is pinned to
`annette` (top-level `name:` in `docker-compose.yml`), so `docker compose -p annette …` works from
any directory.

Image versions are pinned in `deploy.sh` (`BACKEND_VERSION` for the backend services and
demo-ignition, `FRONTEND_VERSION` for the frontend). Backend images are built locally with
`./build-local.sh` from the repo root; the frontend image is pulled from Docker Hub.

## Development mode

```bash
cd deploy/docker
./deploy.sh            # wait until all 6 infrastructure services are healthy
cd ../..
./run-local.sh         # tmux session "annette-dev": 9 services + api-gateway from sbt
./run-ignition.sh      # seed demo data over gRPC (dev-mode ports)
```

## Full mode

```bash
cd deploy/docker
./deploy.sh full       # brings up everything in dependency order; demo-ignition runs last
```

`demo-ignition` is a one-shot service: it seeds demo data (1000 persons, org structures, catalogs,
authorizations, Keycloak users) once the six target services and Keycloak are healthy, then exits 0.
An **exited** `demo-ignition` container is the success state. Re-running `./deploy.sh full` is safe —
all loaders upsert, so seeding is idempotent.

## Access URLs (full mode)

| Component            | URL / port                                   | Credentials                     |
|----------------------|----------------------------------------------|---------------------------------|
| Frontend (Traefik)   | http://localhost:8500                        | `kristina.fisher` / `abc`       |
| REST API via Traefik | http://localhost:8500/api/annette/v1/…       | Bearer token from Keycloak      |
| Frontend (direct)    | http://localhost:8501                        | —                               |
| api-gateway (direct) | http://localhost:8502                        | —                               |
| Keycloak             | http://localhost:8080 (`/realms/AnnetteDemo`) | console admin `admin` / `admin` |
| Camunda              | http://localhost:3090                        | `camunda` / `camunda`           |
| OpenSearch           | https://localhost:9200 (self-signed)         | `admin` / `admin`               |
| MinIO console        | http://localhost:9001                        | —                               |
| Traefik dashboard    | http://localhost:8400                        | —                               |
| pgAdmin (tools)      | http://localhost:15433                       | `postgres@example.com` / `postgres` |
| OS Dashboards (tools)| http://localhost:5601                        | —                               |
| Postgres             | localhost:5432                               | `postgres` / `postgres`         |
| Cassandra            | localhost:9042                               | —                               |
| Backend services     | localhost:8510–8518 (gRPC debug)             | —                               |

## Smoke test (full mode)

```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/realms/AnnetteDemo/protocol/openid-connect/token" \
  -d "grant_type=password&client_id=annette-console&username=kristina.fisher&password=abc" \
  -H "Content-Type: application/x-www-form-urlencoded" | jq -r .access_token)
curl -s -X POST localhost:8500/api/annette/v1/person/findPersons \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"offset":0,"size":2,"filter":""}'        # expect 200, {"total":1000,...}
```

## Teardown

```bash
docker compose -p annette stop          # keep data volumes
docker compose -p annette down          # remove containers, keep data volumes
docker compose -p annette down -v       # remove containers AND all data (destructive!)
```

## Troubleshooting

- **OpenSearch silently blocks index writes** after a container restart: the disk flood-stage
  watermark re-arms and projections retry with `cluster_block_exception ... read_only_allow_delete`.
  Fix:

  ```bash
  curl -k -u admin:admin -X PUT https://localhost:9200/_cluster/settings \
    -H 'Content-Type: application/json' \
    -d '{"transient":{"cluster.routing.allocation.disk.watermark.low":"95%","cluster.routing.allocation.disk.watermark.high":"97%","cluster.routing.allocation.disk.watermark.flood_stage":"99%"}}'
  curl -k -u admin:admin -X PUT https://localhost:9200/_all/_settings \
    -H 'Content-Type: application/json' -d '{"index.blocks.read_only_allow_delete": null}'
  ```

- First `./deploy.sh full` pulls/builds images and may take a while; subsequent runs reuse them.
- `demo-ignition` logs: `docker logs annette-demo-ignition-1`.
