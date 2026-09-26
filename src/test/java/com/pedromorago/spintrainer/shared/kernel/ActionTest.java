package com.pedromorago.spintrainer.shared.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pedromorago.spintrainer.api.model.ActionDto;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ActionTest {

    /** The domain enum and the one generated from the spec have the same codes: if the spec changes, this fails. */
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
