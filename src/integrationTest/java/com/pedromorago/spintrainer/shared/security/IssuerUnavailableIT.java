package com.pedromorago.spintrainer.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.pedromorago.spintrainer.support.ApiIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Si el JWKS de Supabase no responde (caída o SUPABASE_URL mal configurada), el cliente recibe un 503 que lo dice, no
 * un 401 "falta el token" que la web interpretaría como sesión cerrada.
 */
@TestPropertySource(properties = "spin-trainer.auth.jwk-set-uri=http://127.0.0.1:1/auth/v1/.well-known/jwks.json")
class IssuerUnavailableIT extends ApiIntegrationTest {

    @Test
    void anUnreachableJwksIsA503Problem() {
        MvcTestResult result = mvc.get()
                .uri("/api/v1/situations")
                .header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID()))
                .header("X-Correlation-Id", "jwks-down-1")
                .exchange();

        assertThat(result).hasStatus(503).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                        {"type":"urn:spin-trainer:unavailable","status":503,"instance":"/api/v1/situations",
                         "correlationId":"jwks-down-1"}""");
    }

    @Test
    void withoutATokenItIsStillA401() {
        assertThat(mvc.get().uri("/api/v1/situations")).hasStatus(401);
    }
}
