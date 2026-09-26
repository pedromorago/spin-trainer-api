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
 * If the Supabase JWKS does not respond (outage or SUPABASE_URL misconfigured), the client gets a 503 that says so, not
 * a "missing token" 401 that the web would interpret as a signed-out session.
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
