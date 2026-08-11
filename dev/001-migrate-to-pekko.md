# 001 — Meta-prompt: Generate Pekko migration step prompts

## Role

You are an experienced enterprise architect and senior Scala developer with deep expertise in the Akka / Lagom / Apache Pekko stack. You produce precise, executable engineering prompts for other agents.

## Task

Generate a step-by-step migration playbook for the Annette Platform CE repository. Concretely:

1. Read `dev/migrate-to-pekko.md` — this is the **canonical analysis and source of truth**. Every fact you cite must come from there. Do not invent file paths, line counts, or dependencies.
2. Create the directory `dev/migration/`.
3. Generate exactly **13 numbered step-prompt files** in `dev/migration/` plus one `README.md` index, following the structure in §E.
4. Each step prompt must be a self-contained instruction that another agent can execute against the repository to perform one vertical slice of the migration.

Do **not** execute the migration yourself. Do **not** modify source files. Your only output is the 13 prompts + the README.

## A. Hard constraints (non-negotiable)

1. **Public REST contract is frozen.** The API gateway's existing REST endpoints — every path, HTTP method, status code, and JSON response shape declared in `api-gateway/api-gateway/conf/routes` (≈330 routes) and implemented by the 28 controllers — must remain byte-for-byte compatible. The frontend depends on them. The gateway becomes a pure REST→gRPC translator.
2. **Inter-service communication is gRPC** (Apache Pekko gRPC 1.2.0). Every `pathCall` in a Lagom `Service` descriptor becomes an `rpc` in a `.proto` file. Lagom service clients (`serviceClient.implement[T]`) are replaced by generated Pekko gRPC client stubs.
3. **Dual-protocol gateway during the transition.** The gateway supports both Lagom service-client calls (for un-migrated services) and Pekko gRPC client calls (for migrated services) until slice 013 removes the Lagom path. Each service slice owns its own gateway-client swap.
4. **One long-lived feature branch; one conventional commit per step.** Every step prompt ends by instructing the executor to stage only the files it touched and commit with a message in the form `<type>(<scope>): <subject>`. No PRs until slice 013.
5. **Cassandra data is preserved.** Event journal and read-side tables are not migrated or rewritten. Snapshot handling per decision D5 below.
6. **No scope creep.** If `dev/migrate-to-pekko.md` does not list a file or concern, do not add it to a step prompt. Note open questions in the step's "Out of scope" section instead.

## B. Locked decisions (already resolved — do not re-litigate)

The analysis (`dev/migrate-to-pekko.md` §9) lists 7 open decisions. They are resolved as follows. Bake these into every relevant step prompt as fixed assumptions.

| ID | Decision | Resolution |
|---|---|---|
| **D1** | DTO representation | **play-json at the gateway; protobuf intra-cluster.** The 585 files using `play.api.libs.json` stay unchanged. Protobuf messages are authored only for the gRPC service contracts in the `*-api` modules. The gateway translates JSON ↔ protobuf at the boundary. |
| **D2** | Dev-mode service discovery | **Fixed ports per service in `application.dev.conf`.** Drop Lagom's `runAll`/`ServiceLocator`. Each microservice runs as its own process (closer to production). Document the port table in slice 002. |
| **D3** | Circuit breaker | **Drop `lagom.circuit-breaker.*`.** Do not replace. gRPC's built-in `UNAVAILABLE` retry/backoff via `withBackoff` covers transient failures. resilience4j may be added later if measured need (out of scope for this migration). |
| **D4** | Read-side offset store | **Pekko Projection with the Cassandra offset store** (`pekko.projection.cassandra`). Reuses the existing Cassandra cluster; no new Postgres dependency for offsets. |
| **D5** | Snapshot migration | **Discard existing snapshots at each service's cutover; replay from the journal.** Entities rehydrate by replaying events on first load after migration (one-time slower startup). The cutover happens inside each service slice and is called out in its verification section. Rationale: avoids the Pekko 1.0.3 snapshot-format pitfall documented in the analysis. |
| **D6** | gRPC reflection / gRPC-Web | **Reflection: enabled** (cheap, aids debugging with `grpcurl`). **gRPC-Web: not enabled** (frontend stays on REST per constraint A1). |
| **D7** | Public REST contract | **Byte-for-byte preservation** of paths, status codes, JSON shapes (already implied by A1; restated here for completeness). |

## C. Source-of-truth map (where to find each kind of fact in `dev/migrate-to-pekko.md`)

When authoring a step prompt, cite these sections rather than paraphrasing from memory:

| Need | Section |
|---|---|
| File counts per Lagom/Akka abstraction | §3.1, §3.2, §3.3, §3.4, §3.5 |
| Target architecture diagram | §4 |
| Service contract translation (pathCall → rpc) | §4.1 |
| Read-side translation (ReadSideProcessor → Pekko Projection) | §4.2 |
| Entity translation (`AkkaTaggerAdapter` removal) | §4.3 |
| Per-layer build/api/microservice/gateway guidance | §5.1–§5.11 |
| Exact dependency substitution (`lagomScaladsl*` → `org.apache.pekko:*`) | §6 |
| Risks to call out per slice | §7 |
| Phase ordering and effort estimates | §8 |
| File-count reality check (what each prompt touches) | §11 |

## D. The 13 step prompts you must generate

Produce exactly these files in `dev/migration/`. Names are fixed. The "Depends on" column dictates the DAG; each prompt's "Prerequisites" section must require the prior step's commit to exist on the branch.

| # | File | Scope (one-line) | Depends on | Analysis refs |
|---|---|---|---|---|
| 001 | `001-spike-and-decisions.md` | Phase 0 validation spikes: Pekko Projection at-least-once + offset recovery against a dev keyspace; `eventsByTag` tag-name continuity (must equal Lagom's `AggregateEventTag.sharded` output byte-for-byte so historical events stay queryable); Play-on-Pekko version compat proof-of-concept (minimal Play app + `pekko.http.server.preview.enable-http2 = on` + one Pekko gRPC route); design doc for gRPC exception→`Status` mapping covering the `AnnetteTransportException` hierarchy in `core/core/.../exception/*`. Output = locked decisions + go/no-go checkpoint. | — | §7 risks 1, 2, 3, 4, 6; §8 Phase 0 |
| 002 | `002-build-and-gateway-foundations.md` | `project/plugins.sbt` (add `pekko-grpc-sbt-plugin` 1.2.0; **keep** `lagom-sbt-plugin` temporarily); `build.sbt` dep edits per §6 table; `project/Dependencies.scala` Pekko artifacts; Play upgrade to a Pekko-based release (verify exact version in slice 001's spike output); `AnnetteApiLoader` rewritten to drop `LagomConfigComponent`/`LagomServiceClientComponents` and introduce a gRPC-client-factory pattern (still wired with macwire); fixed-port dev-mode table in each `application.dev.conf`. Do **not** migrate any service yet. | 001 | §5.1, §5.5, §6, §3.4 |
| 003 | `003-shared-core-pekko.md` | `core/microservice-core`: rewrite `CassandraDao` to use `CqlSession` (DataStax driver); rewrite `CassandraQuillDao` to use `CassandraAsyncContext` (drop `quill-cassandra-lagom`, keep `quill-cassandra`); rewrite `SimpleEventHandling` to drop `EventStreamElement`; introduce a `Tagger[T]` helper replacing `AggregateEventTagger`; add a `ProjectionBase` trait that Pekko Projection handlers extend; add a `microservice-core-pekko` compatibility module so un-migrated services keep compiling on Lagom. | 002 | §3.5, §5.4, §11 |
| 004 | `004-authorization-service.md` | **Template slice** — establishes the recipe all later service slices copy. Author `authorization-api/src/main/protobuf/authorization.proto` from the existing `AuthorizationServiceApi` trait; rewrite `AuthorizationServiceImpl` against the generated trait (the `entityRef.ask[Confirmation]` body is unchanged); replace 2× `AkkaTaggerAdapter.fromLagom` calls (`RoleEntity`, `AssignmentEntity`); port the `*DbEventProcessor` and `*IndexEventProcessor` classes to Pekko Projection handlers; update `authorization/conf/application*.conf` (akka→pekko keys, Artery port 17355, drop `lagom.*`); update `authorization/src/main/resources/reference.conf` serialization bindings to `pekko.jackson-json`; swap the gateway's `AuthorizationService` client to a generated gRPC stub. End-to-end smoke test against live Cassandra + OpenSearch. | 003 | §3.2, §4, §5.2, §5.3, §5.7, §5.8, §8 Phase 2 |
| 005 | `005-persons-service.md` | `persons` slice (2 entities incl. `PersonEntity`, attribute schema). Same recipe as 004. | 004 | §8 Phase 3 |
| 006 | `006-principal-groups-service.md` | `principal-groups` slice. Same recipe. | 004 | §8 Phase 3 |
| 007 | `007-org-structure-service.md` | `org-structure` slice (4 entities incl. the 477-line `HierarchyEntity` — call out its complexity). Same recipe. | 004 | §8 Phase 3 |
| 008 | `008-subscriptions-service.md` | `subscriptions` slice. Same recipe. | 004 | §8 Phase 3 |
| 009 | `009-service-catalog-service.md` | `service-catalog` slice (5 entities — largest cluster-sharding footprint after CMS). Same recipe. | 004 | §8 Phase 3 |
| 010 | `010-application-service.md` | `application` slice (4 entities incl. `ApplicationEntity`, `LanguageEntity`, `TranslationEntity`, `TranslationJsonEntity`). Same recipe. | 004 | §8 Phase 3 |
| 011 | `011-cms-service.md` | `cms` slice (10 entities — largest in the codebase). Also: replace Alpakka S3 → Pekko Connectors S3 in `cms-api/CmsStorage.scala` and `api-gateway/cms-api-gateway/.../CmsS3Helper.scala` (imports `org.apache.pekko.connectors.s3.*`). Same recipe otherwise. | 004 | §3.3 (Alpakka row), §5.3, §8 Phase 3 |
| 012 | `012-bpm-repository-service.md` | `bpm-repository` slice — **Postgres-only, no Cassandra entities, no event sourcing**. Easiest slice. The `BpmRepositoryServiceApiImpl` body (Slick queries) is unchanged; only the API contract (proto + gRPC) and the loader change. Same recipe otherwise. | 004 | §3.1, §8 Phase 3 |
| 013 | `013-ignition-and-final-cleanup.md` | Migrate `ignition/demo-ignition` off `StandaloneLagomClientFactory` to generated Pekko gRPC clients (rewrite `IgnitionLagomClient.scala`); remove `lagom-sbt-plugin` from `project/plugins.sbt`; delete the `microservice-core-pekko` compatibility shim from slice 003 (fold into `microservice-core`); sweep all remaining `lagom.*` config keys across `conf/*.conf` and `deploy/docker/env/*.env` and `deploy/k8s/config.yml`; remove the dual-protocol gateway dead code (drop Lagom service-client path now that no service uses it); update `AGENTS.md`, `README.md`, `deploy/docker/README.md` to reflect new dev commands (no more `sbt runAll`). Final full-suite verification. | 005–012 all merged | §5.10, §5.11, §8 Phase 4 |

Slices 005–012 may execute in any order after 004 but all must complete before 013.

## E. Step-prompt recipe (every generated file must follow this skeleton)

Each `dev/migration/NNN-*.md` file is a prompt addressed to *the agent that will execute it*. Use this exact section order. Keep prose tight; prefer bullet lists.

```
# NNN — <short title>

## Role
You are a senior Scala/Pekko engineer performing one vertical slice of the Annette Platform
migration off Lagom/Akka to Apache Pekko + Pekko gRPC. You execute against the working tree
of the repository directly.

## Prerequisites
- Branch `<branch-name>` is checked out (the migration feature branch).
- Steps <list prior step numbers> are already committed on this branch.
- Repo state at start of this step: <one paragraph — what compiles, what's been migrated,
  what's still on Lagom>. Be specific.
- External services running: Cassandra 3.11 on :9042, OpenSearch 2.8 on :9200, (others if
  relevant), reachable with the env-var prefixes from AGENTS.md.

## Goal
<One paragraph stating the single outcome this step delivers.>

## Files in scope
<Explicit list of file paths this step may modify, grouped by sub-project. Pull these from
`dev/migrate-to-pekko.md` §11 / the relevant §5 subsection. State "no other files" explicitly.>

## Locked assumptions (do not re-decide)
- <Restate the subset of D1–D7 and constraints A1–A6 that apply to this step.>

## Steps
<Ordered, numbered, concrete. Each step names the file being edited and the change. Reference
the analysis section that justifies the change. Do not paste the analysis verbatim — cite it.>

1. ...
2. ...
...

## Verification
<Concrete commands the executor must run and the expected outcome. Include at minimum:>
- `sbt compile` succeeds for the modules in scope.
- `sbt '<module>/testOnly <spec FQN>'` — name the specific spec(s) that must pass.
- For service slices: a manual or scripted smoke test that exercises the migrated service
  through the gateway's REST endpoint(s) end-to-end.
- For service slices: confirm the snapshot-discard (D5) step ran and entities rehydrated from
  the journal without errors.
- Commit only after all verification passes.

## Commit
Stage only the files listed in "Files in scope". Commit message:
  <type>(<scope>): <subject>
where:
- <type> ∈ {feat, refactor, build, chore, docs}
- <scope> = the service or module name (e.g. `authorization`, `microservice-core`, `api-gateway`)
- <subject> = imperative, ≤72 chars
Example: `feat(authorization): migrate service to pekko gRPC`

## Out of scope
<Explicit non-goals. List anything an over-eager executor might be tempted to do that belongs
to a later step. Examples: "Do not touch other services' loaders", "Do not remove the
microservice-core-pekko shim (that's step 013)", "Do not change the routes file".>

## References
- `dev/migrate-to-pekko.md` §<specific subsections> — for the analysis behind each change.
```

### Rules for filling the recipe

- **Prerequisites must be falsifiable.** "Step 003 is committed" is falsifiable (`git log --oneline | grep` for the commit subject). "The codebase is in good shape" is not.
- **Files in scope must be exhaustive.** If a step edits N files, list all N. Pull the list from the analysis; do not approximate. Add "no other files" as the last bullet.
- **Steps must reference the analysis by section number,** not paraphrase. Example: "Per `dev/migrate-to-pekko.md` §5.4, swap `CassandraLagomAsyncContext` for `CassandraAsyncContext`." Then give the concrete edit.
- **Verification commands must be runnable as-is** from the repo root. No "run the tests" hand-waving — name the spec FQN.
- **One commit per step.** No mid-step commits, no multi-commit steps.
- **Conventional commit format only** per §A4.

## F. README.md for `dev/migration/`

Also produce `dev/migration/README.md` containing:

1. One-paragraph summary of the playbook.
2. The 13-row table from §D above (numbered, with dependencies).
3. A DAG in ASCII showing dependencies (001 → 002 → 003 → 004 → {005..012} → 013).
4. A "How to execute" section: check out the migration branch, run prompts in order, commit after each, only proceed to the next step after the current step's verification passes.
5. A "Status" table with one row per step and columns: `Step | Status | Commit SHA | Notes`. The executor fills this in as they go.
6. A pointer back to `dev/migrate-to-pekko.md` as the source of truth and `dev/001-migrate-to-pekko.md` (this file) as the generator.

## G. Quality gates (verify before declaring generation complete)

Before you stop, run through this checklist. If any item fails, fix the generated prompts.

1. **Coverage:** Every service module in `build.sbt`'s root `.aggregate(...)` list (the 9 microservices) is the primary subject of exactly one slice in 004–012. Cross-check against `build.sbt`.
2. **No overlap:** No two step prompts list the same file in "Files in scope", except `api-gateway/api-gateway/app/biz/lobachev/annette/api_gateway/AnnetteApiLoader.scala` (touched by 002 for bootstrap and by 004–012 each for one client swap — make this explicit in each).
3. **DAG validity:** Every "Depends on" referenced in a Prerequisites section corresponds to a real step number, and the graph has no cycles.
4. **Decision consistency:** Each step that touches a decision-relevant area restates the locked decision. Specifically:
   - Any step authoring `.proto` files → restates D1.
   - Any step editing `application.dev.conf` → restates D2 (fixed ports) and lists the port.
   - Any step touching gateway client construction → restates D3 (no circuit breaker replacement).
   - Any step porting a `ReadSideProcessor` → restates D4 (Cassandra offset store).
   - Any service slice (004–012) → restates D5 (discard snapshots at cutover) in its Verification section.
   - Any step authoring a `.proto` service → restates D6 (reflection enabled) by adding the reflection service to the gRPC handler concat.
   - Any gateway-touching step → restates D7 (REST contract frozen) in Out of scope.
5. **Artery port migration:** Every step that touches `application.dc.conf` or `application.k8s.conf` calls out the Artery port change 25520 → 17355 and any k8s container-port edits in `deploy/k8s/*.yml`.
6. **Verification realism:** Every Verification section names a concrete sbt command and at least one spec FQN (existing) or a smoke-test script (new, to be written by the executor). No "tests pass" without naming them.
7. **Reference integrity:** Every `§<number>` citation in a step prompt exists in `dev/migrate-to-pekko.md`. (Re-read the analysis's table of contents to confirm.)
8. **Commit message hygiene:** Every Commit section uses the format from §E. Scope matches the slice.
9. **No Lagom residue after 013:** Slice 013's "Files in scope" must include every remaining `lagom.*` config reference and the `lagom-sbt-plugin` line in `project/plugins.sbt`.

## H. Anti-patterns to avoid

- **Do not** paste the analysis into step prompts. Cite sections; let the executor open `dev/migrate-to-pekko.md`.
- **Do not** invent file paths. If the analysis does not name a file, do not name it.
- **Do not** bundle two services in one slice. The granularity is fixed at 13.
- **Do not** skip the spike (001) — its outputs unlock decisions that 002 and 004 depend on. If the spike fails, the playbook halts.
- **Do not** instruct the executor to delete Lagom files until slice 013 (except within a single service's own `*-api` module during its slice, where the old `Service` trait is replaced by the generated one).
- **Do not** change the public REST routes, status codes, or response JSON shapes. The gateway is a translator, not a refactor target.
- **Do not** introduce resilience4j, Akka CoordinatedShutdown tuning, or any library not in the analysis's dependency table (§6).
- **Do not** migrate to Scala 3 or Pekko 2.0 milestones. Stay on Scala 2.13.x and Pekko 1.x.

## I. Output directive

Produce these 14 files and nothing else:

```
dev/migration/README.md
dev/migration/001-spike-and-decisions.md
dev/migration/002-build-and-gateway-foundations.md
dev/migration/003-shared-core-pekko.md
dev/migration/004-authorization-service.md
dev/migration/005-persons-service.md
dev/migration/006-principal-groups-service.md
dev/migration/007-org-structure-service.md
dev/migration/008-subscriptions-service.md
dev/migration/009-service-catalog-service.md
dev/migration/010-application-service.md
dev/migration/011-cms-service.md
dev/migration/012-bpm-repository-service.md
dev/migration/013-ignition-and-final-cleanup.md
```

After writing all 14 files, run through §G's checklist and report which gates pass. Do not commit anything — the human operator will review the generated playbook before any step is executed.
