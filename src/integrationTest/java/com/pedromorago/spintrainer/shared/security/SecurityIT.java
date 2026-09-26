package com.pedromorago.spintrainer.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.pedromorago.spintrainer.support.ApiIntegrationTest;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Every API route requires a valid session JWT; the rejections are Problem Details with correlationId. */
class SecurityIT extends ApiIntegrationTest {

    static final UUID USER = UUID.randomUUID();

    @Test
    void withoutTokenIs401Problem() {
        MvcTestResult result = mvc.get().uri("/api/v1/situations").exchange();

        assertThat(result)
                .hasStatus(401)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                        {"type":"urn:spin-trainer:unauthorized","title":"Unauthorized","status":401,
                         "instance":"/api/v1/situations"}""");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.correlationId")
                .isEqualTo(result.getResponse().getHeader("X-Correlation-Id"));
    }

    static Stream<Arguments> invalidTokens() {
        Instant past = Instant.now().minusSeconds(3600);
        return Stream.of(
                Arguments.of(
                        "caducado",
                        JWT.token(
                                USER,
                                c -> c.issueTime(Date.from(past.minusSeconds(60)))
                                        .expirationTime(Date.from(past)))),
                Arguments.of("otro emisor", JWT.token(USER, c -> c.issuer("https://evil.example/auth/v1"))),
                Arguments.of("rol anon", JWT.token(USER, c -> c.claim("role", "anon"))),
                Arguments.of("clave desconocida", JWT.tokenSignedByUnknownKey(USER)),
                Arguments.of("HS256", JWT.hs256Token(USER)),
                Arguments.of("sin firma", JWT.unsignedToken(USER)),
                Arguments.of("basura", "no-es-un-jwt"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTokens")
    void invalidTokenIs401WithInvalidTokenError(String description, String token) {
        MvcTestResult result = mvc.get()
                .uri("/api/v1/situations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange();

        assertThat(result)
                .hasStatus(401)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");
        assertThat(result).bodyJson().extractingPath("$.type").isEqualTo("urn:spin-trainer:unauthorized");
    }

    @Test
    void validTokenReachesTheApi() {
        // Nonexistent route: if it responds 404 (and not 401), authentication passed.
        MvcTestResult result = mvc.get()
                .uri("/api/v1/does-not-exist")
                .header(HttpHeaders.AUTHORIZATION, bearer(USER))
                .exchange();

        assertThat(result).hasStatus(404).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.type").isEqualTo("urn:spin-trainer:not-found");
    }

    @Test
    void echoesAValidCorrelationIdEvenOnErrors() {
        MvcTestResult result = mvc.get()
                .uri("/api/v1/situations")
                .header("X-Correlation-Id", "web-1234")
                .exchange();

        assertThat(result).hasStatus(401).hasHeader("X-Correlation-Id", "web-1234");
        assertThat(result).bodyJson().extractingPath("$.correlationId").isEqualTo("web-1234");
    }
}
