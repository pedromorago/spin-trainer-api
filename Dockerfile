# syntax=docker/dockerfile:1
# API image for Docker Desktop, the spin-trainer-qa suite (docker compose) and deployment.
# Builds with the Gradle wrapper and runs the jar in layers (dependencies, loader and code separately): a code change
# does not re-upload the dependencies. Configuration only through environment variables (README, "Configuración").

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties openapi.yaml ./
COPY gradle ./gradle
COPY src/main ./src/main
RUN --mount=type=cache,target=/root/.gradle \
    sh ./gradlew --no-daemon --console=plain bootJar \
    && java -Djarmode=tools -jar build/libs/spin-trainer-api-*.jar extract --layers --launcher --destination /extracted

FROM eclipse-temurin:21-jre
RUN groupadd --system spring && useradd --system --gid spring --uid 10001 spring
WORKDIR /app
COPY --from=build /extracted/dependencies/ ./
COPY --from=build /extracted/spring-boot-loader/ ./
COPY --from=build /extracted/snapshot-dependencies/ ./
COPY --from=build /extracted/application/ ./
USER spring
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
