# 002 — Docker deployment: development and full modes

## Role

You are an experienced DevOps engineer working in the Annette Platform CE repository. You execute
against the working tree directly and are fluent in Docker Compose v2 (profiles, healthchecks,
`depends_on` conditions).

## Thinking

Before starting, ask clarifying questions if the information below is not enough to obtain a
high-quality result. Ask exactly as many questions as necessary — briefly and to the point — and do
not start making decisions until you get the answers. The decisions in §"Locked decisions" were
already resolved with the maintainer: do **not** re-ask or re-litigate them.

## Task

Restructure the Docker deployment in `deploy/docker/` into two deployments driven by Compose
profiles, and update the deployment documentation:

- **development** (default, no profile) — core infrastructure only, for local development where
  the backend services run from sbt (`./run-local.sh` at the repo root).
- **full** (`full` profile) — the complete platform: infrastructure + Traefik + api-gateway + all 9
  backend services + frontend (prebuilt image) + demo-data initialisation via `demo-ignition`.

## Current state (verified facts — cite, don't re-discover)

- `deploy/docker/docker-compose.yml`
  - Default (no-profile) services: `postgres` (15.3, build `./postgres`, healthcheck), `pgadmin`,
    `cassandra` (3.11.15, healthcheck), `os-node1` (OpenSearch 2.8, healthcheck, HTTPS `:9200`,
    admin/admin, self-signed), `os-dashboards`, `minio` (no healthcheck; console `:9001`, API
    `:9002`), `keycloak` (22.0.0, build `./keycloak`, imports realm `AnnetteDemo` via
    `start-dev --import-realm`, **no healthcheck**), `camunda` (7.19.0, build `./camunda`,
    REST `:3090`, **no healthcheck**), `traefik` (dashboard `:8400`, edge `:8500`).
  - `profiles: [all]` services: `frontend` (`annetteplatform/annette-frontend:${FRONTEND_VERSION}`,
    nginx `:8080`, host `:8501`), `api-gateway` (host `:8502`→9000), and the 9 backend services
    (`application`…`principal-groups`, host ports `8510`–`8518`→9000, env files `env/ms.env` etc.,
    `depends_on` cassandra/os healthy; `bpm-repository` depends on postgres).
  - Traefik routes `PathPrefix(`/`)` → frontend and `PathPrefix(`/api`)` → api-gateway. The frontend
    nginx serves **static files only** (no proxy) — the SPA relies on the same-origin `/api` path,
    so **Traefik is required** whenever frontend + gateway run together.
  - Backend containers listen on container port `9000` (`annette.http.port` in `application.dc.conf`
    via `JAVA_OPTS` in `env/ms.env`).
- `deploy/docker/deploy.sh` — sets `FRONTEND_VERSION=0.5.1`, `BACKEND_VERSION=0.6.0-RC`, runs
  `docker-compose --project-name annette up -d`. It **never activates the `all` profile** — the
  current "full" path documented in the README does not actually work.
- `deploy/docker/demo-ignition.sh` — `docker run -it --network annette_default` of
  `annetteplatform/demo-ignition:0.6.0` (version mismatch with `deploy.sh`) with
  `-Dconfig.file=/opt/docker/conf/application.demo.conf`. That conf points the Pekko gRPC clients at
  the compose service names on port 9000 (`application`, `service-catalog`, `persons`,
  `org-structure`, `authorization`, `principal-groups`) and takes the Keycloak URL from
  `KEYCLOAK_URL` (default `http://localhost:8080` — must be overridden to `http://keycloak:8080`
  inside the compose network).
- Versions: `build.sbt` sets `version = "0.6.0"`; published images are `annetteplatform/<service>:0.6.0`
  and frontend `annetteplatform/annette-frontend:0.5.1`.
- `deploy/docker/README.md` — describes a profile selection that `deploy.sh` doesn't implement; no
  full-deployment or access-URL documentation.
- Env files in `deploy/docker/env/`: `ms.env`, `api-gw.env`, `bpm-repository.env`, `cms-minio.env`,
  `keycloak.env`, `camunda.env`, `play-secret.env` — already correct for the dc/dockered services;
  not expected to change.
- Demo credentials (realm `AnnetteDemo`, public client `annette-console`): user
  `kristina.fisher` / `abc`; Keycloak master admin `admin`/`admin`.

## Locked decisions (do not re-decide)

| ID | Decision |
|---|---|
| **D1 — layout** | One `docker-compose.yml` + Compose profiles. Default (no profile) = development infra. `full` profile adds traefik, frontend, api-gateway, the 9 backend services, and a one-shot `demo-ignition` service. `tools` profile adds pgAdmin and OpenSearch Dashboards (dev conveniences). Traefik carries **both** `tools` and `full` profiles (it is the single-origin router for the full stack; harmless standalone in `tools`). |
| **D2 — image source** | All application images are **pulled published images**, never built here: `annetteplatform/*:${BACKEND_VERSION}` and `annetteplatform/annette-frontend:${FRONTEND_VERSION}`. Versions live in exactly one place — `deploy.sh` — pinned to `BACKEND_VERSION=0.6.0` (fixes the `0.6.0-RC`/`0.6.0` mismatch) and `FRONTEND_VERSION=0.5.1`. |
| **D3 — frontend** | Prebuilt frontend image only. Do not build from `../annette-front` and do not modify that repo. |
| **D4 — development scope** | Development = `postgres`, `cassandra`, `os-node1`, `minio`, `keycloak`, `camunda` only. `pgadmin`, `os-dashboards` move to the `tools` profile. |
| **D5 — ignition** | `demo-ignition` runs as a **one-shot compose service** in the `full` profile (image `annetteplatform/demo-ignition:${BACKEND_VERSION}`, `JAVA_OPTS=-Dconfig.file=/opt/docker/conf/application.demo.conf`, `KEYCLOAK_URL=http://keycloak:8080`, `restart: "no"`, `depends_on` the api-gateway). The standalone `deploy/docker/demo-ignition.sh` becomes obsolete — delete it. |
| **D6 — docs** | Rewrite `deploy/docker/README.md` only. Do not touch root `README.md`, `AGENTS.md`, `deploy/k8s/`, or the frontend repo. |
| **D7 — project name** | Keep the compose project pinned to `annette` (top-level `name: annette` in the compose file) so volumes/network keep their names independently of the invoking directory. |

## Files in scope

- `deploy/docker/docker-compose.yml` — restructure per D1–D5, D7.
- `deploy/docker/deploy.sh` — mode argument + versions per D2.
- `deploy/docker/demo-ignition.sh` — delete (D5).
- `deploy/docker/README.md` — rewrite (D6).
- No other files. (Adjust an entry in `deploy/docker/env/*` only if the new wiring strictly
  requires it, and justify it in the commit body.)

## Steps

1. **`docker-compose.yml` — profiles.** Add top-level `name: annette` (D7). Assign profiles:
   `pgadmin`, `os-dashboards` → `tools`; `traefik` → `tools` + `full`; `frontend`, `api-gateway`,
   the 9 backend services, and the new `demo-ignition` service → `full`. Remove the now-dead `all`
   profile. Default services (D4) carry no profile.
2. **Healthchecks + dependency wiring.** Add healthchecks where missing so `depends_on` conditions
   work: `keycloak` (verify a working endpoint on the 22.0 image — `curl -f
   http://localhost:8080/health/ready` works with health enabled; add `--health-enabled=true` to
   the `command` if needed), `camunda` (e.g. `curl -f
   http://localhost:8080/engine-rest/engine/default`), `minio` (`curl -f
   http://localhost:9000/minio/health/live`). Make `api-gateway` depend (healthy) on cassandra,
   os-node1, postgres, keycloak, camunda, minio; keep/extend the backend services' existing
   conditions; `bpm-repository` on postgres. For `demo-ignition` use the strongest condition the
   api-gateway supports (healthcheck preferred; otherwise `service_started` plus a small retry loop
   in the service command — verify which works).
3. **`demo-ignition` service.** Add per D5. It must join the compose network as a client of the
   backend gRPC services (its `application.demo.conf` already targets service names on :9000) and
   reach Keycloak at `http://keycloak:8080` via `KEYCLOAK_URL`. It exits 0 after seeding — that
   must not be treated as a stack failure (do not use `--abort-on-container-exit` in deploy.sh).
4. **`deploy.sh`.** Rewrite as the single entry point: `./deploy.sh` (or `./deploy.sh dev`) starts
   the development infra; `./deploy.sh full` starts infrastructure + full profile (compose brings
   the whole graph up in one `up -d`, ignition included); support starting `tools` alongside
   (`./deploy.sh dev tools` / `./deploy.sh full tools`, or document `COMPOSE_PROFILE=tools` — pick
   one, keep it simple). Versions pinned per D2. Use `docker compose` (v2 syntax, no dash).
5. **Delete `deploy/docker/demo-ignition.sh`.** Its role is covered by the compose service.
6. **`README.md` rewrite.** Cover: prerequisites (Docker + Compose v2); the two deployment modes
   and exact commands; what each mode starts; access URLs and ports table (frontend
   `http://localhost:8500`, api-gateway direct `:8502`, gateway API via Traefik
   `http://localhost:8500/api/...`, Keycloak `:8080`, Camunda `:3090`, OpenSearch HTTPS `:9200`
   admin/admin, MinIO console `:9001`, pgAdmin `:15433`, OS Dashboards `:5601`, Traefik dashboard
   `:8400`, service debug ports `8510`–`8518`); demo credentials (`kristina.fisher`/`abc`, Keycloak
   admin `admin`/`admin`); where dev-mode (`run-local.sh` / `run-ignition.sh`) fits vs the full
   docker stack; ignition semantics (runs automatically with `full`, idempotent upserts, safe to
   re-run); teardown incl. data wipe (`docker compose -p annette down -v`); and the OpenSearch
   flood-stage watermark gotcha from AGENTS.md (symptom + fix). Keep it tight — tables over prose.

## Verification

Run these against live Docker; all must pass before committing:

1. `docker compose -f deploy/docker/docker-compose.yml config --profiles` and `docker compose ...
   config --services` resolve correctly: no profile → exactly the 6 infra services; `--profile
   full` → infra + traefik + frontend + api-gateway + 9 services + demo-ignition; `--profile tools`
   → infra + pgadmin + os-dashboards + traefik.
2. From a clean state (`docker compose -p annette down -v` first): `cd deploy/docker && ./deploy.sh`
   — all 6 infra services reach `healthy`, and nothing else starts.
3. `./deploy.sh full` — the whole graph comes up; the `demo-ignition` container runs and exits 0;
   re-running `./deploy.sh full` is idempotent (ignition upserts succeed again).
4. Smoke through the single origin: obtain a token from `http://localhost:8080/realms/AnnetteDemo/
   protocol/openid-connect/token` (client `annette-console`, `kristina.fisher`/`abc`) and call
   `POST http://localhost:8500/api/annette/v1/person/findPersons` with `{"offset":0,"size":2,
   "filter":""}` — expect 200 and `"total": 1000` (seeded data). Frontend loads at
   `http://localhost:8500` and login works.
5. No `deploy/docker/demo-ignition.sh` remains; `deploy/docker/README.md` commands match the new
   `deploy.sh` interface exactly.

## Out of scope

- `deploy/k8s/`, `run-local.sh`, `run-ignition.sh`, `build-local.sh`, the `../annette-front` repo,
  root `README.md` / `AGENTS.md`, publishing or building any image.
- Version bumps beyond pinning `0.6.0` / `0.5.1` (manual policy, not automation).

## Commit

Stage only the files listed in §"Files in scope" and commit once:

```
build(docker): split deployment into development and full profiles
```
