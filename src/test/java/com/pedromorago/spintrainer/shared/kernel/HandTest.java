package com.pedromorago.spintrainer.shared.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class HandTest {

    @ParameterizedTest
    @ValueSource(strings = {"AA", "22", "AKs", "AKo", "T9s", "32o", "A2s"})
    void acceptsCanonicalHands(String code) {
        assertThat(Hand.isCanonical(code)).isTrue();
        assertThat(Hand.of(code).code()).isEqualTo(code);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"AAs", "AAo", "AK", "KAs", "AKx", "1Ks", "aks", "AKs ", "A", "AKsX"})
    void rejectsEverythingElse(String code) {
        assertThat(Hand.isCanonical(code)).isFalse();
        assertThatThrownBy(() -> Hand.of(code)).isInstanceOf(DomainException.class);
    }

    @Test
    void theGridHas169DistinctHandsRowByRow() {
        List<Hand> all = Hand.all();

        assertThat(all).hasSize(169);
        assertThat(new HashSet<>(all)).hasSize(169);
        assertThat(all.subList(0, 3)).extracting(Hand::code).containsExactly("AA", "AKs", "AQs");
        assertThat(all.get(13).code()).isEqualTo("AKo");
        assertThat(all.getLast().code()).isEqualTo("22");
    }

    @Test
    void readingOrderIsPairsThenSuitedThenOffsuit() {
        assertThat(Stream.of("AKo", "22", "A2s", "KQs", "AA", "AKs", "32o")
                        .map(Hand::of)
                        .sorted()
                        .map(Hand::toString))
                .containsExactly("AA", "22", "AKs", "A2s", "KQs", "AKo", "32o");
    }
}
