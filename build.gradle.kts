import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    `java-test-fixtures`
    jacoco
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.spotless)
}

group = "com.pedromorago"
version = "0.2.0"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

repositories { mavenCentral() }

// OpenAPI-first (ADR-0004): las interfaces *Api y los DTOs se generan desde openapi.yaml en cada build.
val generatedOpenApi = layout.buildDirectory.dir("generated/openapi")

openApiGenerate {
    generatorName = "spring"
    inputSpec = layout.projectDirectory.file("openapi.yaml")
    outputDir = generatedOpenApi
    apiPackage = "com.pedromorago.spintrainer.api"
    modelPackage = "com.pedromorago.spintrainer.api.model"
    modelNameSuffix = "Dto"
    generateApiTests = false
    generateModelTests = false
    generateApiDocumentation = false
    generateModelDocumentation = false
    configOptions.putAll(
        mapOf(
            "interfaceOnly" to "true",
            // Sin implementación por defecto: un controller que no implemente una operación no compila.
            "skipDefaultInterface" to "true",
            "useTags" to "true",
            "useSpringBoot4" to "true",
            "useJackson3" to "true",
            "openApiNullable" to "false",
            "useBeanValidation" to "true",
            "useResponseEntity" to "true",
            "documentationProvider" to "none",
            "annotationLibrary" to "none",
            "hideGenerationTimestamp" to "true",
            "sourceFolder" to "src/main/java",
        ),
    )
}

sourceSets.main { java.srcDir(generatedOpenApi.map { it.dir("src/main/java") }) }

tasks.compileJava { dependsOn(tasks.openApiGenerate) }

tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }

dependencies {
    val springBootBom = platform(SpringBootPlugin.BOM_COORDINATES)
    implementation(springBootBom)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)

    testImplementation(springBootBom)
    testImplementation(libs.spring.boot.starter.test)

    testFixturesImplementation(springBootBom)
}

// Dos suites: `test` (dominio, casos de uso, arquitectura; sin Docker) e `integrationTest` (Spring + Testcontainers).
testing {
    suites {
        named<JvmTestSuite>("test") { useJUnitJupiter() }

        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation(testFixtures(project()))
                implementation(libs.spring.boot.starter.webmvc.test)
            }
            targets.all { testTask.configure { shouldRunAfter(tasks.test) } }
        }
    }
}

configurations.named("integrationTestImplementation") { extendsFrom(configurations.testImplementation.get()) }
configurations.named("integrationTestRuntimeOnly") { extendsFrom(configurations.testRuntimeOnly.get()) }

tasks.withType<Test>().configureEach {
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.check { dependsOn(testing.suites.named("integrationTest"), tasks.jacocoTestReport) }

// Cobertura de las dos suites; el código generado no cuenta.
tasks.jacocoTestReport {
    dependsOn(tasks.test, tasks.named("integrationTest"))
    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")).include("*.exec"))
    classDirectories.setFrom(
        sourceSets.main.get().output.classesDirs.asFileTree.matching {
            exclude("com/pedromorago/spintrainer/api/**")
        },
    )
    reports {
        xml.required = true
        html.required = true
    }
}

spotless {
    java {
        target("src/*/java/**/*.java")
        palantirJavaFormat(
            libs.versions.palantir.java.format
                .get(),
        )
        removeUnusedImports()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
    format("misc") {
        target("*.md", "*.yaml", ".gitignore", ".gitattributes", ".github/**/*.yml", "src/**/*.sql", "src/**/*.yaml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
