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
                            "2 entradas inválidas en hands",
                            List.of(
                                    new FieldError("hands.AAs", "mano inválida"),
                                    new FieldError("hands.KK", "acción no permitida")));
                case NOT_FOUND -> DomainException.notFound("Situación/stack desconocido: x@25");
                case CONFLICT -> DomainException.conflict("El rango está en la versión 3; recarga");
                case NO_RANGE -> DomainException.noRange("Sin rango para btn_open@8");
            };
        }

        @GetMapping("/_test/boom")
        void boom() {
            throw new IllegalStateException("detalle interno que no debe salir");
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
                        {"detail":"2 entradas inválidas en hands","errors":[
                          {"field":"hands.AAs","message":"mano inválida"},
                          {"field":"hands.KK","message":"acción no permitida"}]}""");
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
        assertThat(result)
                .bodyJson()
                .isLenientlyEqualTo("{\"type\":\"urn:spin-trainer:validation\",\"errors\":[{\"field\":\"name\"}]}");
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
                .isLenientlyEqualTo("{\"errors\":[{\"field\":\"correct\",\"message\":\"campo no permitido\"}]}");
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
                .isLenientlyEqualTo("{\"errors\":[{\"field\":\"colours.AA\",\"message\":\"valor no válido\"}]}");
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
