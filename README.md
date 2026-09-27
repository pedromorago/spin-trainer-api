# spin-trainer-api

Spin Trainer API: a preflop range trainer for Spin & Go (3-max and heads-up). A study and QA portfolio project.
Frontend in [spin-trainer-web](https://github.com/pedromorago/spin-trainer-web), black-box tests (and the portfolio overview: what they found and how) in
[spin-trainer-qa](https://github.com/pedromorago/spin-trainer-qa). Context, architecture and ADRs: `spin-trainer-web/docs/`.

Spring Boot 4.1 · Java 21 · Gradle (Kotlin DSL) · OpenAPI-first with openapi-generator · Spring Security (Supabase JWT)
· Postgres + Flyway · JdbcClient · ArchUnit · Testcontainers.

## Requirements

- JDK 21 (Gradle downloads it if missing) and Docker (Docker Desktop with WSL 2 on Windows) for the integration tests.
- No need to install Gradle: the wrapper is used (`gradlew` / `gradlew.bat`).

## Commands

| Windows | Linux/macOS | What it does |
|---|---|---|
| `.\gradlew.bat check` | `./gradlew check` | Formatting, unit tests, architecture, integration (Testcontainers), coverage and mutation testing |
| `.\gradlew.bat pitest` | `./gradlew pitest` | Mutation testing only (PIT, ADR-0017); report in `build/reports/pitest` |
| `.\gradlew.bat spotlessApply` | `./gradlew spotlessApply` | Applies formatting (palantir-java-format, ktlint) |
| `.\gradlew.bat bootTestRun` | `./gradlew bootTestRun` | API at `http://localhost:8080` with an already migrated Postgres in Docker; without `SUPABASE_URL` it prints a development token |
| `.\gradlew.bat bootRun` | `./gradlew bootRun` | API against the database and Supabase set in the environment variables |

## Docker image

```
docker build -t spin-trainer-api .
```

A GraalVM native executable (ADR-0018): Spring AOT and `nativeCompile` in a GraalVM 25 build stage, run as an
unprivileged user with the `prod` profile (JSON logs). With 0.1 CPU and 512 MB it is ready in about 4 s, migrations
included, and uses about 64 MiB (the JVM took almost two minutes there). The build takes about four minutes and a few
GB of memory (on Windows, give Docker Desktop at least 8 GB). It is the image that spin-trainer-qa starts
(`env/docker-compose.yml`) and the one that is deployed. `./gradlew nativeCompile` builds the same executable outside
Docker if GraalVM is installed (`GRAALVM_HOME`).

## Deployment

Render's free plan, no card on file (ADR-0018): `render.yaml` (web service in Frankfurt, image from GHCR, readiness
check) and `.github/workflows/deploy.yml`, which builds the native image of the commit CI validated on `main`, pushes it
to GHCR, triggers Render's deploy hook with that image and waits until it answers. Without the
`RENDER_DEPLOY_HOOK_URL` secret it only builds and pushes the image. The listening port comes from `PORT` when the
platform sets it (8080 otherwise). First-time setup (Supabase, roles, secrets, Vercel): `spin-trainer-web/docs/DEPLOY.md`.

## Configuration

| Variable | Example | Purpose |
|---|---|---|
| `SUPABASE_URL` | `https://<ref>.supabase.co` | JWT issuer; the API verifies the signature against its JWKS (ES256 asymmetric keys). Required |
| `DB_URL` | `jdbc:postgresql://<host>:5432/postgres?sslmode=require` | Postgres (on Supabase, the *session pooler*; TLS required) |
| `DB_APP_PASSWORD` / `DB_MIGRATOR_PASSWORD` | | Passwords for `spin_app` (the API) and `spin_migrator` (Flyway) |
| `DB_APP_USER` / `DB_MIGRATOR_USER` | `spin_app` / `spin_migrator` | Login users. Behind Supabase's pooler: `spin_app.<ref>` / `spin_migrator.<ref>` |
| `DB_APP_ROLE` | `spin_app` | Role the migrations grant permissions to; optional unless the role was renamed in `bootstrap.sql` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,https://spin-trainer-web.vercel.app` | Frontend origins |
| `SPRING_PROFILES_ACTIVE` | `prod` | In deployment: JSON logs (ECS) with `correlationId` |

## Database

Schema `app` (not exposed to PostgREST), migrated by Flyway (`src/main/resources/db/migration`, sequential numbering:
schema and seed in the order they are applied). Two least-privilege roles (ADR-0015):

- `spin_migrator`: owns the schema; used only by Flyway.
- `spin_app`: the API's role; reads the catalog and the reference ranges, writes only where needed.

### Reference ranges

The 73 tables of `Tablasmentov3.pdf` (16 situations, one range per stack) are in `reference-ranges.json`, one line
per row of the 13×13 grid with the explicit-action hands; the rest take the implicit action (FOLD, or CHECK if FOLD
is not possible). `V5__seed_reference_ranges.sql` loads them with those same JSON objects, and `ReferenceSeedIT` checks,
by migrating an empty database, that the result is exactly the file and that every spot in the catalog has a range.

The PDF's "3H OS call" table (page 19) is not a coloured range but one threshold per hand: call an open-shove when the
effective stack is at most that many BB. Its 169 cells are in `reference-os-call-thresholds.json`, and V7 turns them
into the situation `bb_vs_sb_os` (BB vs SB open-shove, the 3-max counterpart of `hu_bb_vs_os`) with one range per stack
(20, 15, 12, 10, 8, 6 and 4 BB). `ReferenceThresholdsTest` checks that those 7 ranges are exactly the derivation:
80 reference ranges in all.

The file was extracted from the PDF by matching each cell's fill color against its table's legend (the exact colors of
the original spreadsheet) and reviewed by overlaying the reconstruction on the original. The web app (mock) and
spin-trainer-qa (oracles) keep copies with their own check, as with the contract. Changing a range = a new migration
and the file in the same commit; the test forces them to match.

The roles are created once per environment with `src/main/resources/db/bootstrap/bootstrap.sql` (it has the
instructions); the integration tests run that same script against a Testcontainers Postgres 17 and check the
permission matrix.

## Errors

Every error response is a Problem Details object (RFC 9457, `application/problem+json`) with `type`
`urn:spin-trainer:<type>` (`validation`, `unauthorized`, `not-found`, `conflict`, `no-range`, `unsupported`,
`unavailable`, `internal`),
`correlationId` (the one from the `X-Correlation-Id` header, which is accepted or generated) and, on 400s, per-field `errors`.
That includes URLs rejected before any controller (Spring Security's firewall: `;`, encoded slashes). Messages are in
Spanish, those of the spec's constraints too (`ConstraintMessages`: not Hibernate Validator's, which follow the JVM's
locale).

Public without a token: `/actuator/health` (and its probes) and `/actuator/info`, which only says the commit the image
was built from (the deploy waits for it).

## Contract

`openapi.yaml` is the source of truth (ADR-0004). Every build generates the `*Api` interfaces and the DTOs
(`build/generated/openapi`); the controllers implement them, so the code cannot drift from the spec.
An API change starts in the spec; the web app pulls the copy with `npm run spec:sync`.

## Endpoints (`/api/v1`, all with `Authorization: Bearer <JWT>`)

| Method | Path | What it does |
|---|---|---|
| GET | `/situations` | Catalog of the 17 situations (ETag) |
| GET | `/ranges/default`, `/ranges/default/{situation}/{stack}` | Reference ranges (ETag) |
| GET | `/ranges/user`, `/ranges/user/{situation}/{stack}` | The user's custom ranges |
| PUT | `/ranges/user/{situation}/{stack}` | Creates (`version: 0` → 201) or replaces version N (200); 409 if it changed. Versions keep counting after a delete |
| DELETE | `/ranges/user/{situation}/{stack}` | Reverts to the reference range (204, idempotent) |
| POST | `/quiz/attempts` | Records an answer; the server grades it (201; 422 without a range) |
| GET | `/quiz/attempts` | Attempts, newest first, with cursor pagination |
| GET | `/stats/hands`, `/stats/progress` | Aggregates per hand and per day (IANA time zone) |

Full contract in `openapi.yaml`. With `bootTestRun`:

```
curl -H "Authorization: Bearer <token printed at startup>" http://localhost:8080/api/v1/situations
```

## Structure

```
openapi.yaml                           contract (source of truth)
gradle/libs.versions.toml              versions
src/main/java/com/pedromorago/spintrainer/
  situation/ range/ quiz/ stats/       modules: domain · application (port.in, port.out) · adapter (in.rest, out.persistence)
  shared/                              kernel · security · web · config
src/main/resources/db/                 migration/ (Flyway) · bootstrap/ (roles, once per environment)
src/test/java                          domain, use cases and ArchUnit (no Spring or Docker)
src/integrationTest/java               full API: Testcontainers, real JWTs, responses validated against the spec
src/testFixtures/java                  test Postgres and JWT issuer (suites and bootTestRun)
```

Architecture, decisions (ADR-0001..0018) and context: `spin-trainer-web/docs/`.
