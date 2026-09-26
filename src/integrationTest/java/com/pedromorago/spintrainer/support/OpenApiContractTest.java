package com.pedromorago.spintrainer.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** El validador de contrato no aprueba en vacío: detecta cuerpos y estados que la spec no admite. */
class OpenApiContractTest {

    static final OpenApiContract CONTRACT = OpenApiContract.load();
    static final String SITUATION = """
            {"key":"btn_open","label":"BTN Open","format":"3max","hero":"BTN","priorActions":[],
             "stacks":[25,20],"actions":["MR_4B_C","FOLD"]}""";

    @Test
    void acceptsAValidBody() {
        assertThat(CONTRACT.violations("GET", "/situations", 200, "[" + SITUATION + "]"))
                .isEmpty();
    }

    @Test
    void rejectsBodiesThatBreakTheSchema() {
        assertThat(CONTRACT.violations("GET", "/situations", 200, "[" + SITUATION.replace("[25,20]", "[12.3]") + "]"))
                .as("stack que no es múltiplo de 0,5")
                .isNotEmpty();
        assertThat(CONTRACT.violations(
                        "GET", "/situations", 200, "[" + SITUATION.replace("\"hero\":\"BTN\",", "") + "]"))
                .as("falta un campo obligatorio")
                .isNotEmpty();
        assertThat(CONTRACT.violations("GET", "/situations", 200, "[" + SITUATION.replace("}", ",\"extra\":1}") + "]"))
                .as("campo no declarado (additionalProperties: false)")
                .isNotEmpty();
        assertThat(CONTRACT.violations("GET", "/situations", 401, "{\"title\":\"Unauthorized\",\"status\":401}"))
                .as("Problem sin type")
                .isNotEmpty();
    }

    @Test
    void rejectsUndeclaredStatusesAndUnexpectedBodies() {
        assertThat(CONTRACT.violations("GET", "/situations", 418, "{}")).isNotEmpty();
        assertThat(CONTRACT.violations("GET", "/situations", 304, "[]")).isNotEmpty();
        assertThat(CONTRACT.violations("GET", "/situations", 304, "")).isEmpty();
    }
}
