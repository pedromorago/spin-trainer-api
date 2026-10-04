# spin-trainer-api

Spin Trainer API. Rules shared by the three repos, summarized here so this repo is self-contained.
Project source of truth: `spin-trainer-web/docs/` (context, `ARCHITECTURE.md`, ADRs 0001..0024).

## Global rules (summary)
- Portfolio quality > speed. ADRs are closed; they are reopened only for a concrete, justified flaw (new ADR).
- Spin & Go only (3-max and HU, 17 situations). Effective range = custom if it exists, otherwise the reference one (ADR-0012).
- Supabase only issues the JWT; the API is the only data path. Tables in the `app` schema, not exposed to PostgREST.
- Reference ranges in the DB via Flyway migrations (versioned seed, V9) with their source in `reference-ranges.json`,
  generated from `reference-ranges-recipe.json`: examples, not a school's charts (ADR-0024). `ExampleRangesTest` and
  `ReferenceSeedIT` require them to match; web and QA keep copies. Quiz attempts = immutable events.
- OpenAPI-first: `openapi.yaml` changes before the code (and the web copy, with `npm run spec:sync`).
- Gradle (Kotlin DSL), never Maven. Discarded: OWASP ZAP, load testing, Pact, pgTAP.
- Everything in English (ADR-0021): code, comments, documentation (README, ADRs, CONTRIBUTING.md, docs/, OpenAPI descriptions), developer-facing messages, the app's UI and data, the API's error messages, and the test report (test titles, Allure names, assertion descriptions, Gherkin features).
- Commits **always in Pedro's name** (author and committer: `Pedro Morago López-Vázquez <pedromoragolv@gmail.com>`; check `git config user.name/user.email` before committing). Conventional Commits, no co-author or attribution trailer.
- Pedro's environment: Windows 10/11 (commands with `gradlew.bat`; nothing that depends on bash).

## Stack and commands
Spring Boot 4.1 (ADR-0014) · Java 21 · Spring Security 7 · Jackson 3 · JdbcClient without JPA (ADR-0015) · Flyway · ArchUnit ·
JUnit 6 + AssertJ · Testcontainers 2 · Spotless (palantir-java-format) · JaCoCo · PIT.

```
./gradlew check          # formatting + test + integrationTest + coverage (requires Docker)
./gradlew spotlessApply  # format
./gradlew bootTestRun     # local API: Postgres in Docker and a printed development token
./gradlew bootRun         # with the real environment variables
```
Before committing: `./gradlew check` must pass. Versions only in `gradle/libs.versions.toml` (no version if Boot's BOM manages it).

## Architecture (enforced by `ArchitectureTest`; if it changes, change it there and in `spin-trainer-web/docs/ARCHITECTURE.md`)
- Modules `situation`, `range`, `quiz`, `stats`, each with `domain` · `application` (`port.in`, `port.out`, service)
  · `adapter.in.rest` · `adapter.out.persistence`. `shared`: `kernel` (value objects + `DomainException`), `security`, `web`, `config`.
- `domain` and `shared.kernel`: plain Java (no Spring, Jakarta, Jackson or JDBC). Across modules, only `application.port.in` and `domain`.
- Code generated from the spec in `com.pedromorago.spintrainer.api` (`*Api` interfaces) and `.api.model` (`*Dto`); never
  edited or committed. Used only by the controllers (`adapter.in.rest`), which implement those interfaces and map to the domain.
- Business errors: `DomainException` (`VALIDATION`, `NOT_FOUND`, `CONFLICT`, `NO_RANGE`); the web layer translates them into Problem Details.
- The effective action of a hand is computed only with `range/domain/RangeRules#actionFor` (same as `domain/range.js` in the web app).
- Persistence: `JdbcClient` + explicit SQL in `adapter.out.persistence`; tables in `app.*`. New migration = next
  number (never edit an applied one) with its `GRANT`s to `${app_role}` and the matching row in `DatabaseRolesIT`.
- Dates: injected `Clock`, truncated to milliseconds (what a POST/PUT returns is what a GET will return).
- Native image (ADR-0018): the `Dockerfile` builds it with Spring AOT + GraalVM. Reflection, resources or proxies that AOT
  cannot see need `RuntimeHints`; the QA suite running against the image is the check. `processAot` resolves the
  placeholders with dummy values: conditions are fixed at build time, values are read at runtime.

## Tests
- Suites: `test` (JUnit 6 + AssertJ, no Spring or Docker), `integrationTest` (`*IT` classes extending
  `ApiIntegrationTest`: full app, Testcontainers Postgres with the real roles, real JWTs from `TestJwtIssuer`),
  `testFixtures` (`PostgresTestDatabase`, `TestJwtIssuer`).
- Every IT response is validated with `CONTRACT.assertResponse(method, specPath, result)`.
- Data the API cannot write (reference ranges, attempts with a chosen date): `TestData`, as administrator. ITs
  that replace or remove reference ranges do so on the shared database; `ReferenceSeedIT` uses its own.
- Isolation: a new user (UUID) per test; shared data is prepared in an idempotent `@BeforeEach`.
- JaCoCo: ≥ 90 % on domain, use cases and kernel; ≥ 85 % overall (enforced by `check`).
- PIT (ADR-0017): mutation score ≥ 95 % on the same packages with the unit tests (enforced by `check`; today 100 %).
  A surviving mutant usually means a missing boundary value: add the test rather than lowering the bar.
