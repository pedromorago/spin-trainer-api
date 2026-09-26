package com.pedromorago.spintrainer.range.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RangeSourceTest {

    /** The codes are the contract's `source` values (ADR-0012). */
    @Test
    void codesAreTheContractValues() {
        assertThat(RangeSource.DEFAULT.code()).isEqualTo("default");
        assertThat(RangeSource.USER.code()).isEqualTo("user");
    }
}
