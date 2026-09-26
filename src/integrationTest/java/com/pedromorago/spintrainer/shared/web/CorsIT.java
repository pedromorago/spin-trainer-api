package com.pedromorago.spintrainer.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.pedromorago.spintrainer.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** The browser can only call from the configured origins; preflight requests do not carry a token. */
class CorsIT extends ApiIntegrationTest {

    @Test
    void preflightFromTheWebOriginIsAllowed() {
        assertThat(mvc.options()
                        .uri("/api/v1/ranges/user/btn_open/25")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "authorization,content-type,x-correlation-id"))
                .hasStatusOk()
                .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
                .hasHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
    }

    @Test
    void preflightFromAnotherOriginIsRejected() {
        assertThat(mvc.options()
                        .uri("/api/v1/situations")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .hasStatus(403)
                .doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    @Test
    void responsesExposeTheCorrelationIdToTheBrowser() {
        MvcTestResult result = mvc.get()
                .uri("/api/v1/situations")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .exchange();

        assertThat(result).hasStatus(401).hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173");
        assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS))
                .contains("X-Correlation-Id");
    }
}
