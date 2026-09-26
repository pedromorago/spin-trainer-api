# syntax=docker/dockerfile:1
# API image (ADR-0018): a GraalVM native executable, for the spin-trainer-qa suite (docker compose) and for Render.
# It starts in well under a second and needs a fraction of the JVM's memory, which is what makes a free 0.1 vCPU
# instance usable (the JVM took almost two minutes to start there). Configuration only through environment variables
# (README, "Configuration").

FROM ghcr.io/graalvm/native-image-community:25 AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties openapi.yaml ./
COPY gradle ./gradle
COPY src/main ./src/main
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon --console=plain nativeCompile

# glibc at least as new as the build image's (2.39): Debian 13.
FROM debian:trixie-slim
RUN useradd --system --uid 10001 spring
COPY --from=build /workspace/build/native/nativeCompile/spin-trainer-api /app/spin-trainer-api
USER spring
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
# The commit being built (deploy.yml): /actuator/info reports it, so the deploy can tell the new version is serving.
ARG REVISION=unknown
ENV APP_REVISION=${REVISION}
ENTRYPOINT ["/app/spin-trainer-api"]
