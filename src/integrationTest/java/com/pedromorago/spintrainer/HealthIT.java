package com.pedromorago.spintrainer;

import static org.assertj.core.api.Assertions.assertThat;

import com.pedromorago.spintrainer.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;

class HealthIT extends ApiIntegrationTest {

    @Test
    void healthIsPublicAndHidesDetails() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson()
                .isEqualTo("{\"status\":\"UP\",\"groups\":[\"liveness\",\"readiness\"]}");
    }

    @Test
    void probesArePublic() {
        assertThat(mvc.get().uri("/actuator/health/liveness")).hasStatusOk();
        assertThat(mvc.get().uri("/actuator/health/readiness")).hasStatusOk();
    }
}
