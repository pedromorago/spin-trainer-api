package com.pedromorago.spintrainer;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

/**
 * Architecture verified, not just documented (ADR-0005): hexagonal modular monolith. Each business module has domain ·
 * application (port.in, port.out, services) · adapter (in.rest, out.persistence). Between modules only the inbound
 * ports and the published domain are used; {@code shared} is the common platform.
 */
class ArchitectureTest {

    static final String ROOT = "com.pedromorago.spintrainer";
    static final String GENERATED_API = ROOT + ".api..";
    static final List<String> MODULES = List.of("situation", "range", "quiz", "stats");

    static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT)
            .that(not(resideInAPackage(ROOT + ".testsupport..")));

    @Test
    void domainAndKernelArePlainJava() {
        noClasses()
                .that()
                .resideInAnyPackage("..domain..", "..shared.kernel..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "tools.jackson..",
                        "com.fasterxml..",
                        "java.sql..",
                        "javax.sql..",
                        "..application..",
                        "..adapter..",
                        GENERATED_API)
                .because("el dominio se prueba sin Spring ni base de datos y no conoce el contrato HTTP")
                .check(CLASSES);
    }

    @Test
    void applicationDoesNotKnowAdaptersNorTransport() {
        noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..adapter..",
                        GENERATED_API,
                        "org.springframework.web..",
                        "org.springframework.jdbc..",
                        "jakarta.servlet..")
                .check(CLASSES);
    }

    @Test
    void onlyRestAdaptersUseTheGeneratedContract() {
        noClasses()
                .that()
                .resideOutsideOfPackages("..adapter.in.rest..", GENERATED_API)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(GENERATED_API)
                .check(CLASSES);
    }

    @Test
    void onlyPersistenceAdaptersUseJdbc() {
        noClasses()
                .that()
                .resideOutsideOfPackage("..adapter.out.persistence..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework.jdbc..", "java.sql..", "javax.sql..")
                .check(CLASSES);
    }

    @Test
    void controllersImplementTheGeneratedInterfaces() {
        classes()
                .that()
                .areAnnotatedWith(RestController.class)
                .should()
                .resideInAPackage("..adapter.in.rest..")
                .andShould()
                .implement(resideInAPackage(GENERATED_API))
                .because("así el código no puede desviarse de openapi.yaml (ADR-0004)")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void modulesOnlyTalkThroughInputPortsAndPublishedDomain() {
        for (String module : MODULES) {
            String modulePackage = ROOT + "." + module + "..";
            noClasses()
                    .that()
                    .resideOutsideOfPackage(modulePackage)
                    .should()
                    .dependOnClassesThat(resideInAPackage(modulePackage)
                            .and(not(resideInAnyPackage(
                                    ROOT + "." + module + ".domain..",
                                    ROOT + "." + module + ".application.port.in.."))))
                    .as("el resto de la aplicación solo usa los puertos de entrada y el dominio de " + module)
                    .allowEmptyShould(true)
                    .check(CLASSES);
        }
    }

    @Test
    void sharedDoesNotDependOnBusinessModules() {
        noClasses()
                .that()
                .resideInAPackage(ROOT + ".shared..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(MODULES.stream()
                        .map(module -> ROOT + "." + module + "..")
                        .toArray(String[]::new))
                .check(CLASSES);
    }

    @Test
    void modulesAreFreeOfCycles() {
        slices().matching(ROOT + ".(*)..").should().beFreeOfCycles().check(CLASSES);
    }
}
