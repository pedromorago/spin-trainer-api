package com.pedromorago.spintrainer.shared.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pedromorago.spintrainer.api.model.ActionDto;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ActionTest {

    /** El enum del dominio y el generado desde la spec tienen los mismos códigos: si la spec cambia, esto falla. */
    @Test
    void matchesTheContract() {
        assertThat(Arrays.stream(Action.values()).map(Action::code))
                .containsExactlyElementsOf(Arrays.stream(ActionDto.values())
                        .map(ActionDto::getValue)
                        .toList());
    }

    @Test
    void usesContractCodesThatAreNotJavaNames() {
        assertThat(Action.fromCode("3BET")).isEqualTo(Action.THREE_BET);
        assertThat(Action.THREE_BET_C.code()).isEqualTo("3BET_C");
    }

    @Test
    void rejectsUnknownCodes() {
        assertThatThrownBy(() -> Action.fromCode("3B_CALL")).isInstanceOf(IllegalArgumentException.class);
    }
}
