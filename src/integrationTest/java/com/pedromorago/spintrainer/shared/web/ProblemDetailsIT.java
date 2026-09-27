package com.pedromorago.spintrainer.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.pedromorago.spintrainer.shared.kernel.DomainException;
import com.pedromorago.spintrainer.shared.kernel.DomainException.FieldError;
import com.pedromorago.spintrainer.support.ApiIntegrationTest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Shape of the errors (RFC 9457) with test endpoints that trigger each case. */
@Import(ProblemDetailsIT.Endpoints.class)
class ProblemDetailsIT extends ApiIntegrationTest {

    static final String AUTH = bearer(UUID.randomUUID());

    enum Colour {
        RED,
        BLUE
    }

    record Sample(@NotNull @Size(max = 3) String name, Map<String, Colour> colours) {}

    @RestController
    static class Endpoints {

        @GetMapping("/_test/domain/{kind}")
        void domain(@PathVariable DomainException.Kind kind) {
            throw switch (kind) {
                case VALIDATION ->
                    DomainException.validation(
                            "2 invalid entries in hands",
                            List.of(
                                    new FieldError("hands.AAs", "invalid hand"),
                                    new FieldError("hands.KK", "action not allowed")));
                case NOT_FOUND -> DomainException.notFound("Unknown situation/stack: x@25");
                case CONFLICT -> DomainException.conflict("The range is at version 3; reload");
                case NO_RANGE -> DomainException.noRange("No range for btn_open@8");
            };
        }

        @GetMapping("/_test/boom")
        void boom() {
            throw new IllegalStateException("internal detail that must not leak");
        }

        @PostMapping("/_test/samples")
        Sample create(@Valid @RequestBody Sample sample) {
            return sample;
        }
    }

    private MvcTestResult send(MockMvcRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, AUTH).exchange();
    }

    @ParameterizedTest
    @CsvSource({
        "VALIDATION, 400, urn:spin-trainer:validation, Validation failed",
        "NOT_FOUND,  404, urn:spin-trainer:not-found,  Not found",
        "CONFLICT,   409, urn:spin-trainer:conflict,   Conflict",
        "NO_RANGE,   422, urn:spin-trainer:no-range,   No range"
    })
    void domainErrorsMapToTheirProblemType(String kind, int status, String type, String title) {
        MvcTestResult result = send(mvc.get().uri("/api/v1/_test/domain/" + kind));

        assertThat(result).hasStatus(status).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result)
                .bodyJson()
                .isLenientlyEqualTo(
                        "{\"type\":\"%s\",\"title\":\"%s\",\"status\":%d,\"instance\":\"/api/v1/_test/domain/%s\"}"
                                .formatted(type, title, status, kind));
        assertThat(result).bodyJson().extractingPath("$.correlationId").isNotNull();
    }

    @Test
    void validationErrorsListEachField() {
        assertThat(send(mvc.get().uri("/api/v1/_test/domain/VALIDATION")))
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"detail":"2 invalid entries in hands","errors":[
                          {"field":"hands.AAs","message":"invalid hand"},
                          {"field":"hands.KK","message":"action not allowed"}]}""");
    }

    @Test
    void onlyThe400sCarryErrors() {
        assertThat(send(mvc.get().uri("/api/v1/_test/domain/CONFLICT")))
                .bodyJson()
                .doesNotHavePath("$.errors");
    }

    @Test
    void unexpectedErrorsAre500WithoutInternalDetails() throws Exception {
        MvcTestResult result = send(mvc.get().uri("/api/v1/_test/boom"));

        assertThat(result).hasStatus(500).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.type").isEqualTo("urn:spin-trainer:internal");
        assertThat(result.getResponse().getContentAsString()).doesNotContain("detalle interno");
    }

    @Test
    void beanValidationErrorsNameTheField() {
        MvcTestResult result = send(mvc.post()
                .uri("/api/v1/_test/samples")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"toolong\"}"));

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                        {"type":"urn:spin-trainer:validation","errors":[{"field":"name","message":"size must be at most 3"}]}""");
    }

    // The messages of the spec's constraints are English whatever the client or the JVM speak (ADR-0021): they are the
    // API's own, not Hibernate Validator's, which follow the JVM's language (Spanish on a Spanish Windows machine).
    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "/api/v1/quiz/attempts?limit=0              | limit     | must be ≥ 1",
                "/api/v1/quiz/attempts?limit=201            | limit     | must be ≤ 200",
                "/api/v1/ranges/default/BTN_OPEN/25          | situation | invalid format",
                "/api/v1/stats/hands?stack=0.5              | stack     | must be ≥ 1"
            })
    void constraintMessagesAreEnglishWhateverTheLocale(String uri, String field, String message) {
        MvcTestResult result = send(mvc.get().uri(uri).header(HttpHeaders.ACCEPT_LANGUAGE, "es-ES"));

        assertThat(result).hasStatus(400);
        assertThat(result)
                .bodyJson()
                .isLenientlyEqualTo("{\"errors\":[{\"field\":\"%s\",\"message\":\"%s\"}]}".formatted(field, message));
    }

    // URLs rejected by Spring Security's firewall never reach a controller: they got Spring Boot's own error body.
    // (MockMvc normalizes "//" before the firewall sees it: spin-trainer-qa checks that one against the real server.)
    @Test
    void urlsTheFirewallRejectsAreProblemsToo() {
        MvcTestResult result = send(mvc.get().uri("/api/v1/situations;jsessionid=1"));

        assertThat(result).hasStatus(400).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result)
                .bodyJson()
                .isLenientlyEqualTo(
                        "{\"type\":\"urn:spin-trainer:validation\",\"status\":400,\"detail\":\"Invalid path\"}");
        assertThat(result).bodyJson().extractingPath("$.correlationId").isNotNull();
    }

    @Test
    void unknownPropertiesAreRejected() {
        MvcTestResult result = send(mvc.post()
                .uri("/api/v1/_test/samples")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ok\",\"correct\":true}"));

        assertThat(result).hasStatus(400);
        assertThat(result)
                .bodyJson()
                .isLenientlyEqualTo("{\"errors\":[{\"field\":\"correct\",\"message\":\"field not allowed\"}]}");
    }

    @Test
    void invalidEnumValuesPointToTheMapEntry() {
        MvcTestResult result = send(mvc.post()
                .uri("/api/v1/_test/samples")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ok\",\"colours\":{\"AA\":\"GREEN\"}}"));

        assertThat(result).hasStatus(400);
        assertThat(result)
                .bodyJson()
                .isLenientlyEqualTo("{\"errors\":[{\"field\":\"colours.AA\",\"message\":\"invalid value\"}]}");
    }

    @Test
    void malformedJsonIs400() {
        MvcTestResult result = send(mvc.post()
                .uri("/api/v1/_test/samples")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":"));

        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().isLenientlyEqualTo("{\"errors\":[{\"field\":\"body\"}]}");
    }

    @Test
    void pathVariablesOfTheWrongTypeAre400() {
        MvcTestResult result = send(mvc.get().uri("/api/v1/_test/domain/NOPE"));

        assertThat(result).hasStatus(400);
        assertThat(result)
                .bodyJson()
                .isLenientlyEqualTo("{\"type\":\"urn:spin-trainer:validation\",\"errors\":[{\"field\":\"kind\"}]}");
    }

    @Test
    void otherSpringErrorsKeepTheirStatusAndGetATypeAndCorrelationId() {
        MvcTestResult result = send(mvc.delete().uri("/api/v1/_test/boom"));

        assertThat(result).hasStatus(405).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result)
                .bodyJson()
                .isLenientlyEqualTo(
                        "{\"type\":\"urn:spin-trainer:unsupported\",\"title\":\"Method Not Allowed\",\"status\":405}");
        assertThat(result).bodyJson().extractingPath("$.correlationId").isNotNull();
    }
}
