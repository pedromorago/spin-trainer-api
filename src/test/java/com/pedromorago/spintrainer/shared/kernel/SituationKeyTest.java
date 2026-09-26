package com.pedromorago.spintrainer.shared.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SituationKeyTest {

    @Test
    void acceptsCatalogKeys() {
        assertThat(SituationKey.of("bb_vs_btn_mr_sb_3bet").value()).isEqualTo("bb_vs_btn_mr_sb_3bet");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"BTN_open", "btn-open", "btn open", "../etc"})
    void rejectsAnythingElse(String value) {
        assertThatThrownBy(() -> SituationKey.of(value)).isInstanceOf(DomainException.class);
    }

    @Test
    void rejectsKeysLongerThan64() {
        assertThatThrownBy(() -> SituationKey.of("a".repeat(65))).isInstanceOf(DomainException.class);
    }
}
