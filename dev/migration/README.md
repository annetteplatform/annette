# Pekko migration playbook

This directory contains the 13 step prompts that execute the Annette Platform CE migration from
Akka/Lagom to Apache Pekko + Pekko gRPC. Each step is a self-contained prompt addressed to the
agent that performs that vertical slice against the working tree.

- **Source of truth (analysis):** `dev/migrate-to-pekko.md`
- **Generator (this playbook's design):** `dev/001-migrate-to-pekko.md`
- **Hard constraints & locked decisions:** see `dev/001-migrate-to-pekko.md` §A and §B

The migration is one feature branch, one conventional commit per step, no PRs until step 013.
Slices 005–012 may run in any order after 004, but all must land before 013.

## Steps

| #  | File                                              | Scope (one-line)                                                                                          | Depends on   |
|----|---------------------------------------------------|-----------------------------------------------------------------------------------------------------------|--------------|
| 001 | `001-spike-and-decisions.md`                     | Phase 0 spikes: Pekko Projection offset recovery, `eventsByTag` tag-name continuity, Play-on-Pekko PoC, exception→`Status` mapping design. Output = locked version matrix + go/no-go. | —            |
| 002 | `002-build-and-gateway-foundations.md`           | sbt plugin swap (add `pekko-grpc-sbt-plugin`, keep `lagom-sbt-plugin`), Pekko dependency artifacts, Play upgrade, `AnnetteApiLoader` gRPC-client-factory pattern, fixed-port dev-mode table. | 001          |
| 003 | `003-shared-core-pekko.md`                       | `microservice-core`: `CassandraDao`→`CqlSession`, `CassandraQuillDao`→`CassandraAsyncContext`, `SimpleEventHandling` rewrite, `Tagger[T]` + `ProjectionBase` helpers, `microservice-core-pekko` compatibility shim. | 002          |
| 004 | `004-authorization-service.md`                   | **Template slice.** `authorization` end-to-end (proto, impl, tagger, projection, config, gateway client swap). Establishes the recipe 005–012 copy. | 003          |
| 005 | `005-persons-service.md`                         | `persons` slice (2 entities).                                                                              | 004          |
| 006 | `006-principal-groups-service.md`                | `principal-groups` slice (2 entities).                                                                     | 004          |
| 007 | `007-org-structure-service.md`                   | `org-structure` slice (3 entities incl. 477-line `HierarchyEntity`).                                       | 004          |
| 008 | `008-subscriptions-service.md`                   | `subscriptions` slice (2 entities).                                                                        | 004          |
| 009 | `009-service-catalog-service.md`                 | `service-catalog` slice (5 entities).                                                                      | 004          |
| 010 | `010-application-service.md`                     | `application` slice (4 entities).                                                                          | 004          |
| 011 | `011-cms-service.md`                              | `cms` slice (8 entities, Alpakka S3 → Pekko Connectors S3).                                                | 004          |
| 012 | `012-bpm-repository-service.md`                  | `bpm-repository` slice (Postgres-only, no entities, no event sourcing).                                    | 004          |
| 013 | `013-ignition-and-final-cleanup.md`              | `demo-ignition` off `StandaloneLagomClientFactory`; drop `lagom-sbt-plugin`; sweep `lagom.*` config; delete compatibility shim; remove gateway dual-protocol dead code; doc updates. | 005–012 all |

## DAG

```
                  ┌───────────────┐
                  │ 001  spike    │
                  └───────┬───────┘
                          ▼
                  ┌───────────────┐
                  │ 002  build/gw │
                  └───────┬───────┘
                          ▼
                  ┌───────────────┐
                  │ 003  core     │
                  └───────┬───────┘
                          ▼
                  ┌───────────────┐
                  │ 004  authoriz.│  (template slice)
                  └───────┬───────┘
                          ▼
        ┌─────┬──────┬─────┬──────┬──────┬─────┬─────┬─────┐
        ▼     ▼      ▼     ▼      ▼      ▼     ▼     ▼     ▼
       005   006    007   008    009    010   011   012  (any order)
        persons pr.grps org   subs   svccat app   cms   bpm
        └─────┴──────┴─────┴──────┴──────┴─────┴─────┴─────┘
                          ▼
                  ┌───────────────┐
                  │ 013  cleanup  │
                  └───────────────┘
```

## How to execute

1. **One-time setup:** `git checkout -b feature/migrate-to-pekko`. This is the long-lived
   migration branch. All 13 commits land here.
2. Bring up external services (Cassandra 3.11 on `:9042`, OpenSearch 2.8 on `:9200`,
   Postgres on `:5432` for bpm-repository, Keycloak on `:9090`, MinIO on `:9000`,
   Camunda on `:8082`):
   ```bash
   cd deploy/docker && ./deploy.sh
   ```
3. Open `dev/migration/001-spike-and-decisions.md` and follow it literally. Do not improvise.
4. After each step:
   - Run its **Verification** section commands. All must pass before proceeding.
   - Run its **Commit** section exactly (stage only files in "Files in scope"; commit with
     the specified message).
   - Update the **Status** table below with the commit SHA.
5. If a step's verification fails, fix the issue in the same commit (amend before pushing) —
   do not pile failing commits onto the branch.
6. Step 013 is the merge candidate: only after its full-suite verification passes does the
   branch become eligible for PR.

## Status

| Step | Status     | Commit SHA | Notes |
|------|------------|------------|-------|
| 001  | pending    | —          |       |
| 002  | pending    | —          |       |
| 003  | pending    | —          |       |
| 004  | pending    | —          |       |
| 005  | pending    | —          |       |
| 006  | pending    | —          |       |
| 007  | pending    | —          |       |
| 008  | pending    | —          |       |
| 009  | pending    | —          |       |
| 010  | pending    | —          |       |
| 011  | pending    | —          |       |
| 012  | pending    | —          |       |
| 013  | pending    | —          |       |

## Codebase corrections baked into this playbook

The analysis (`dev/migrate-to-pekko.md`) predates a verification pass. The following
discrepancies were found and the prompts use the verified figures:

- **Controller count is 30, not 28.** The 30 controllers live across 8 gateway subprojects
  under `api-gateway/*-api-gateway/src/main/scala/**`, not under
  `api-gateway/api-gateway/app/.../controller/`. That path does not exist —
  `api-gateway/api-gateway/app/` contains only `AnnetteApiLoader.scala`.
- **Routes file has 287 entries**, not ~330. Path: `api-gateway/api-gateway/conf/routes`.
- **CMS has 8 entities** in `cms/cms`; the "10 entities" figure in the analysis counts
  `SubscriptionEntity` + `SubscriptionTypeEntity` from `cms/subscriptions`, which is its own
  microservice (slice 008). Slice 011 covers 8 entities.
- **`org-structure` has 3 entities** (`HierarchyEntity`, `CategoryEntity`, `OrgRoleEntity`),
  not 4.
- **`service-catalog` has 5 entities** (confirmed): `CategoryEntity`, `ScopeEntity`,
  `ServiceItemEntity`, `ScopePrincipalEntity`, `ServicePrincipalEntity`.
- **`application` has 4 entities** (confirmed): `ApplicationEntity`, `LanguageEntity`,
  `TranslationEntity`, `TranslationJsonEntity`.
- **All 9 `serviceClient.implement[...]` calls** are centralized in
  `api-gateway/api-gateway/app/biz/lobachev/annette/api_gateway/AnnetteApiLoader.scala`
  (lines 177–201). No gateway controller calls `implement[...]` directly; each receives its
  service via Play `@Inject()`.
- **Two API-trait patterns coexist.** Older services (authorization, persons,
  principal-groups, org-structure, subscriptions, cms, bpm-repository) use the single-trait
  pattern `*ServiceApi extends Service` + `*ServiceApiImpl` (server) + `*ServiceImpl` (client
  wrapper). Newer services (application, service-catalog) use a two-trait pattern: plain
  `*Service` + separate `*ServiceLagomApi extends Service` + `*ServiceLagomApiImpl` (server
  adapter) + `*ServiceLagomImpl` (client wrapper) + `*ServiceImpl` (domain impl). Each
  service slice names its actual trait paths.
- **`lagomForkedTestSettings` is not defined in the repo** — it is provided by the Lagom sbt
  plugin and applied at 9 sites in `build.sbt`. Replacement is `Test / fork := true`.
- **Known pre-existing bug** (not caused by this migration): `bpm/bpm-repository/conf/application.k8s.conf`
  sets `service-name = "subscriptions"` (copy-paste). Slice 012 fixes it.
