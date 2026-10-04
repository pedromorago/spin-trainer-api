import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    `java-test-fixtures`
    jacoco
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.spotless)
    alias(libs.plugins.pitest)
    alias(libs.plugins.graalvm.native)
}

group = "com.pedromorago"
version = "0.2.0"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

repositories { mavenCentral() }

// OpenAPI-first (ADR-0004): the *Api interfaces and the DTOs are generated from openapi.yaml on every build.
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
            // No default implementation: a controller that does not implement an operation does not compile.
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
    implementation(libs.spring.boot.starter.security.oauth2.resource.server)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)

    testImplementation(springBootBom)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.archunit)

    testFixturesImplementation(springBootBom)
    testFixturesApi(libs.spring.security.oauth2.jose)
    testFixturesApi(libs.testcontainers.postgresql)
    testFixturesRuntimeOnly(libs.postgresql)
}

// Two suites: `test` (domain, use cases, architecture; no Docker) and `integrationTest` (Spring + Testcontainers).
testing {
    suites {
        named<JvmTestSuite>("test") { useJUnitJupiter() }

        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            dependencies {
                implementation(project())
                implementation(testFixtures(project()))
                implementation(libs.spring.boot.starter.webmvc.test)
                implementation(libs.json.schema.validator)
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

tasks.check { dependsOn(testing.suites.named("integrationTest"), tasks.jacocoTestCoverageVerification, tasks.pitest) }

// The example reference ranges (ADR-0024): rewrites reference-ranges.json from reference-ranges-recipe.json.
// ExampleRangesTest fails while they differ; changing a range also takes a new migration.
tasks.register<JavaExec>("generateReferenceRanges") {
    group = "reference data"
    description = "Writes reference-ranges.json from reference-ranges-recipe.json."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "com.pedromorago.spintrainer.range.ExampleRanges"
    workingDir = projectDir
}

// Coverage of both suites; the code generated from the spec does not count.
val coveredClasses =
    sourceSets.main.map { main ->
        main.output.classesDirs.asFileTree.matching {
            exclude("com/pedromorago/spintrainer/api/**", "org/openapitools/**")
        }
    }

tasks.jacocoTestReport {
    dependsOn(tasks.test, tasks.named("integrationTest"))
    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")).include("*.exec"))
    classDirectories.setFrom(coveredClasses)
    reports {
        xml.required = true
        html.required = true
    }
}

// Same bar as the web (90 % in the domain): domain, kernel and use cases; 85 % overall.
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")).include("*.exec"))
    classDirectories.setFrom(coveredClasses)
    violationRules {
        rule {
            limit { minimum = "0.85".toBigDecimal() }
        }
        rule {
            element = "PACKAGE"
            includes = listOf("*.domain", "*.application", "*.shared.kernel")
            limit { minimum = "0.90".toBigDecimal() }
        }
    }
}

// Native image (ADR-0018): `nativeCompile` turns the AOT-processed app into one executable that starts in milliseconds
// on a free 0.1 vCPU instance. The GraalVM of the build image (GRAALVM_HOME) compiles it; javac stays on the Java 21
// toolchain.
graalvmNative {
    toolchainDetection = false
    binaries.named("main") {
        imageName = "spin-trainer-api"
        // Runs on any x86-64 CPU: the default (x86-64-v3) needs AVX2, and the host's hardware is not guaranteed.
        buildArgs.add("-march=compatibility")
    }
}

// AOT evaluates the auto-configuration conditions at build time, so the placeholders must resolve; these values are
// not kept: at runtime the real ones come from the environment.
tasks.processAot {
    environment(
        mapOf(
            "DB_URL" to "jdbc:postgresql://aot.invalid:5432/aot",
            "DB_APP_PASSWORD" to "aot",
            "DB_MIGRATOR_PASSWORD" to "aot",
            "SUPABASE_URL" to "https://aot.invalid",
        ),
    )
}

// Mutation testing (ADR-0017): do the unit tests fail when a rule changes? Same scope as the 90 % coverage bar; unit
// tests only (no Docker, ~30 s), so check runs it. Report: build/reports/pitest. 95 % leaves room for equivalent mutants.
pitest {
    pitestVersion =
        libs.versions.pitest.core
            .get()
    junit5PluginVersion =
        libs.versions.pitest.junit5
            .get()
    targetClasses =
        setOf(
            "com.pedromorago.spintrainer.*.domain.*",
            "com.pedromorago.spintrainer.*.application.*",
            "com.pedromorago.spintrainer.shared.kernel.*",
        )
    excludedTestClasses = setOf("*ArchitectureTest")
    threads = 4
    outputFormats = setOf("HTML", "XML")
    timestampedReports = false
    mutationThreshold = 95
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
        target(
            "*.md",
            "*.yaml",
            "Dockerfile",
            ".dockerignore",
            ".gitignore",
            ".gitattributes",
            ".github/**/*.yml",
            "src/**/*.sql",
            "src/**/*.yaml",
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}
