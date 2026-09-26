package com.pedromorago.spintrainer;

import com.pedromorago.spintrainer.testsupport.PostgresTestDatabase;
import com.pedromorago.spintrainer.testsupport.TestJwtIssuer;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.boot.SpringApplication;

/**
 * Arranque local con solo Docker instalado: {@code gradlew bootTestRun}. Levanta un Postgres de Testcontainers
 * preparado como producción (roles + Flyway) y, si no hay {@code SUPABASE_URL}, un emisor de JWT local e imprime un
 * token para probar la API con curl, Postman o Newman. Con {@code SUPABASE_URL} valida los tokens reales de la web.
 */
public final class TestSpinTrainerApiApplication {

    static final UUID DEV_USER = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private TestSpinTrainerApiApplication() {}

    public static void main(String[] args) {
        PostgresTestDatabase.shared().springProperties().forEach(System::setProperty);
        String supabaseUrl = System.getenv("SUPABASE_URL");
        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            TestJwtIssuer issuer = TestJwtIssuer.shared();
            System.setProperty("spin-trainer.auth.issuer", issuer.issuer());
            String token = issuer.token(
                    DEV_USER,
                    claims -> claims.expirationTime(Date.from(Instant.now().plus(Duration.ofHours(12)))));
            System.out.printf(
                    "%nToken de desarrollo (12 h, usuario %s):%nAuthorization: Bearer %s%n%n", DEV_USER, token);
        }
        SpringApplication.run(SpinTrainerApiApplication.class, args);
    }
}
