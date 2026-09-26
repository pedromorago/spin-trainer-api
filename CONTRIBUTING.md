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

## Convenciones
- Código generado desde la spec en `com.pedromorago.spintrainer.api` (interfaces `*Api`) y `.api.model` (`*Dto`); nunca se edita ni se versiona.
- Suites: `test` (JUnit sin Spring ni Docker), `integrationTest` (Spring + Testcontainers, clases `*IT`), `testFixtures` (utilidades comunes).
- Tests con AssertJ y `MockMvcTester`.
