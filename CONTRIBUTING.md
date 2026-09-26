# spin-trainer-api

API de Spin Trainer. Reglas comunes a los tres repos, resumidas aquí para que este repo sea autosuficiente.
Fuente de verdad del proyecto: `spin-trainer-web/docs/` (contexto, `ARCHITECTURE.md`, ADRs 0001..0015).

## Reglas globales (resumen)
- Calidad de portfolio > velocidad. ADRs cerrados; solo se reabren con fallo concreto y justificado (ADR nuevo).
- Solo Spin & Go (3-max y HU, 16 situaciones). Rango efectivo = personalizado si existe, si no el de referencia (ADR-0012).
- Supabase solo emite el JWT; la API es el único camino de datos. Tablas en el esquema `app`, no expuesto a PostgREST.
- Rangos de referencia en BD vía migraciones Flyway (seed versionado). Intentos del Quiz = eventos inmutables.
- OpenAPI-first: se cambia `openapi.yaml` antes que el código (y la copia de la web con `npm run spec:sync`).
- Gradle (Kotlin DSL), nunca Maven. Descartados: OWASP ZAP, carga, Pact, pgTAP.
- Commits **siempre a nombre de Pedro** (autor y committer: `Pedro Morago López-Vázquez <pedromoragolv@gmail.com>`;
  verificar `git config user.name/user.email` antes de commitear). Conventional Commits, sin trailer de coautoría ni de atribución.
- Entorno de Pedro: Windows 10/11 (comandos con `gradlew.bat`; nada que dependa de bash).

## Stack y comandos
Spring Boot 4.1 (ADR-0014) · Java 21 · Spring Security 7 · Jackson 3 · JdbcClient sin JPA (ADR-0015) · Flyway · ArchUnit ·
JUnit 6 + AssertJ · Testcontainers 2 · Spotless (palantir-java-format) · JaCoCo.

```
./gradlew check          # formato + test + integrationTest + cobertura (necesita Docker)
./gradlew spotlessApply  # formatear
./gradlew bootRun
```
Antes de commitear: `./gradlew check` en verde. Versiones solo en `gradle/libs.versions.toml` (sin versión si la gestiona el BOM de Boot).

## Arquitectura (la verifica `ArchitectureTest`; si cambia, se cambia ahí y en `spin-trainer-web/docs/ARCHITECTURE.md`)
- Módulos `situation`, `range`, `quiz`, `stats`, cada uno con `domain` · `application` (`port.in`, `port.out`, servicio)
  · `adapter.in.rest` · `adapter.out.persistence`. `shared`: `kernel` (value objects + `DomainException`), `security`, `web`, `config`.
- `domain` y `shared.kernel`: Java puro (sin Spring, Jakarta, Jackson ni JDBC). Entre módulos solo `application.port.in` y `domain`.
- Código generado desde la spec en `com.pedromorago.spintrainer.api` (interfaces `*Api`) y `.api.model` (`*Dto`); nunca se
  edita ni se versiona. Solo lo usan los controllers (`adapter.in.rest`), que implementan esas interfaces y mapean a dominio.
- Errores de negocio: `DomainException` (`VALIDATION`, `NOT_FOUND`, `CONFLICT`, `NO_RANGE`); la web los traduce a Problem Details.
- La acción efectiva de una mano solo se calcula con `range/domain/RangeRules#actionFor` (igual que `domain/range.js` en la web).
- Persistencia: `JdbcClient` + SQL explícito en `adapter.out.persistence`; tablas en `app.*`. Migración nueva = número
  siguiente (nunca editar una aplicada) con sus `GRANT` a `${app_role}` y la fila correspondiente en `DatabaseRolesIT`.
- Fechas: `Clock` inyectado y truncado a milisegundos (lo que devuelve un POST/PUT es lo que devolverá un GET).

## Tests
- Suites: `test` (JUnit 6 + AssertJ, sin Spring ni Docker), `integrationTest` (clases `*IT` que extienden
  `ApiIntegrationTest`: app completa, Postgres de Testcontainers con los roles reales, JWT reales de `TestJwtIssuer`),
  `testFixtures` (`PostgresTestDatabase`, `TestJwtIssuer`).
- Cada respuesta de un IT se valida con `CONTRACT.assertResponse(method, pathDeLaSpec, result)`.
- Datos que la API no puede escribir (rangos de referencia, intentos con fecha): `TestData`, como administrador.
- Aislamiento: un usuario (UUID) nuevo por test; los datos compartidos se preparan en `@BeforeEach` idempotente.
- JaCoCo: ≥ 90 % en dominio, casos de uso y kernel; ≥ 85 % en total (lo exige `check`).
