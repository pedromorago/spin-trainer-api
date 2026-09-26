# spin-trainer-api

API de Spin Trainer: entrenador de rangos preflop para Spin & Go (3-max y heads-up). Proyecto de estudio y portfolio QA.
Frontend en [spin-trainer-web](https://github.com/pedromorago/spin-trainer-web), pruebas de caja negra en
[spin-trainer-qa](https://github.com/pedromorago/spin-trainer-qa). Contexto, arquitectura y ADRs: `spin-trainer-web/docs/`.

Spring Boot 4.1 · Java 21 · Gradle (Kotlin DSL) · OpenAPI-first con openapi-generator · Spring Security (JWT de Supabase)
· Postgres + Flyway · JdbcClient · ArchUnit · Testcontainers.

## Requisitos

- JDK 21 (Gradle lo descarga si falta) y Docker (Docker Desktop con WSL 2 en Windows) para los tests de integración.
- No hace falta instalar Gradle: se usa el wrapper (`gradlew` / `gradlew.bat`).

## Comandos

| Windows | Linux/macOS | Qué hace |
|---|---|---|
| `.\gradlew.bat check` | `./gradlew check` | Formato, tests unitarios, arquitectura, integración (Testcontainers) y cobertura |
| `.\gradlew.bat spotlessApply` | `./gradlew spotlessApply` | Aplica el formato (palantir-java-format, ktlint) |
| `.\gradlew.bat bootRun` | `./gradlew bootRun` | Arranca la API en `http://localhost:8080` |

## Configuración

| Variable | Ejemplo | Para qué |
|---|---|---|
| `SUPABASE_URL` | `https://<ref>.supabase.co` | Emisor de los JWT; la API valida la firma contra su JWKS (claves asimétricas ES256). Obligatoria |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,https://spin-trainer.vercel.app` | Orígenes del frontend |
| `SPRING_PROFILES_ACTIVE` | `prod` | En despliegue: logs JSON (ECS) con `correlationId` |

## Errores

Todas las respuestas de error son Problem Details (RFC 9457, `application/problem+json`) con `type`
`urn:spin-trainer:<tipo>` (`validation`, `unauthorized`, `not-found`, `conflict`, `no-range`, `unsupported`, `internal`),
`correlationId` (el de la cabecera `X-Correlation-Id`, que se acepta o se genera) y, en los 400, `errors` por campo.

## Contrato

`openapi.yaml` es la fuente de verdad (ADR-0004). En cada build se generan las interfaces `*Api` y los DTOs
(`build/generated/openapi`); los controllers las implementan, así que el código no puede desviarse de la spec.
Un cambio de API empieza en la spec; la web trae la copia con `npm run spec:sync`.

## Estructura

```
openapi.yaml                       contrato (fuente de verdad)
gradle/libs.versions.toml          versiones
src/main/java/.../spintrainer/     aplicación
src/test/java                      tests sin Spring ni Docker
src/integrationTest/java           tests con Spring (y Testcontainers)
src/testFixtures/java              utilidades compartidas por las suites
```
