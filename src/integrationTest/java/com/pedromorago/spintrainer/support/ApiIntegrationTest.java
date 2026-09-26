package com.pedromorago.spintrainer.support;

import com.pedromorago.spintrainer.testsupport.PostgresTestDatabase;
import com.pedromorago.spintrainer.testsupport.TestJwtIssuer;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Base de los tests de integración: la aplicación completa con MockMvc sobre Postgres real (Testcontainers, migrado
 * por Flyway con los roles de producción) y JWT reales validados contra el JWKS de {@link TestJwtIssuer}.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class ApiIntegrationTest {

    protected static final TestJwtIssuer JWT = TestJwtIssuer.shared();
    protected static final PostgresTestDatabase DB = PostgresTestDatabase.shared();
    protected static final OpenApiContract CONTRACT = OpenApiContract.load();

    @Autowired
    protected MockMvcTester mvc;

    @DynamicPropertySource
    static void environment(DynamicPropertyRegistry registry) {
        registry.add("spin-trainer.auth.issuer", JWT::issuer);
        DB.springProperties().forEach((key, value) -> registry.add(key, () -> value));
    }

    protected static String bearer(UUID user) {
        return "Bearer " + JWT.tokenFor(user);
    }
}
