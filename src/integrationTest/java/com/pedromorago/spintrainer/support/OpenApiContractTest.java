package com.pedromorago.spintrainer.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** El validador de contrato no aprueba en vacío: detecta cuerpos, formatos, tipos y estados que la spec no admite. */
class OpenApiContractTest {

    static final OpenApiContract CONTRACT = OpenApiContract.load();
    static final String JSON = "application/json";
    static final String PROBLEM = "application/problem+json";
    static final String SITUATION = """
            {"key":"btn_open","label":"BTN Open","format":"3max","hero":"BTN","priorActions":[],
             "stacks":[25,20],"actions":["MR_4B_C","FOLD"]}""";
    static final String PROGRESS_DAY = "{\"date\":\"2026-09-26\",\"attempts\":2,\"correct\":1}";

    @Test
    void acceptsAValidBody() {
        assertThat(CONTRACT.violations("GET", "/situations", 200, JSON, "[" + SITUATION + "]"))
                .isEmpty();
        assertThat(CONTRACT.violations(
                        "GET", "/stats/progress", 200, JSON + ";charset=UTF-8", "[" + PROGRESS_DAY + "]"))
                .isEmpty();
    }

    @Test
    void rejectsBodiesThatBreakTheSchema() {
        assertThat(CONTRACT.violations(
                        "GET", "/situations", 200, JSON, "[" + SITUATION.replace("[25,20]", "[12.3]") + "]"))
                .as("stack que no es múltiplo de 0,5")
                .isNotEmpty();
        assertThat(CONTRACT.violations(
                        "GET", "/situations", 200, JSON, "[" + SITUATION.replace("\"hero\":\"BTN\",", "") + "]"))
                .as("falta un campo obligatorio")
                .isNotEmpty();
        assertThat(CONTRACT.violations(
                        "GET", "/situations", 200, JSON, "[" + SITUATION.replace("}", ",\"extra\":1}") + "]"))
                .as("campo no declarado (additionalProperties: false)")
                .isNotEmpty();
        assertThat(CONTRACT.violations(
                        "GET", "/situations", 401, PROBLEM, "{\"title\":\"Unauthorized\",\"status\":401}"))
                .as("Problem sin type")
                .isNotEmpty();
    }

    @Test
    void checksFormats() {
        assertThat(CONTRACT.violations(
                        "GET", "/stats/progress", 200, JSON, "[" + PROGRESS_DAY.replace("2026-09-26", "ayer") + "]"))
                .as("date")
                .isNotEmpty();
        assertThat(CONTRACT.violations("GET", "/stats/hands", 200, JSON, """
                        [{"situation":"btn_open","stack":25,"hand":"AA","attempts":1,"correct":1,
                          "lastAnsweredAt":"hace un rato"}]"""))
                .as("date-time")
                .isNotEmpty();
    }

    @Test
    void checksTheContentType() {
        assertThat(CONTRACT.violations(
                        "GET", "/situations", 401, JSON, "{\"type\":\"urn:x\",\"title\":\"x\",\"status\":401}"))
                .as("un error como application/json en vez de problem+json")
                .isNotEmpty();
        assertThat(CONTRACT.violations("GET", "/situations", 200, null, "[]")).isNotEmpty();
    }

    @Test
    void rejectsUndeclaredStatusesAndUnexpectedBodies() {
        assertThat(CONTRACT.violations("GET", "/situations", 418, JSON, "{}")).isNotEmpty();
        assertThat(CONTRACT.violations("GET", "/situations", 304, null, "[]")).isNotEmpty();
        assertThat(CONTRACT.violations("GET", "/situations", 304, null, "")).isEmpty();
    }
}
