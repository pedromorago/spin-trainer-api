package com.pedromorago.spintrainer.support;

import com.pedromorago.spintrainer.testsupport.TestJwtIssuer;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Base de los tests de integración: la aplicación completa con MockMvc, validando JWT reales contra el JWKS de
 * {@link TestJwtIssuer} (mismo camino que con Supabase).
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class ApiIntegrationTest {

    protected static final TestJwtIssuer JWT = TestJwtIssuer.shared();

    @Autowired
    protected MockMvcTester mvc;

    @DynamicPropertySource
    static void authProperties(DynamicPropertyRegistry registry) {
        registry.add("spin-trainer.auth.issuer", JWT::issuer);
    }

    protected static String bearer(UUID user) {
        return "Bearer " + JWT.tokenFor(user);
    }
}
