package com.pedromorago.spintrainer.stats.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Aggregated rows cannot have impossible counts (a badly written query would fail here, not in the web). */
class StatRowsTest {

    static final Instant AT = Instant.parse("2026-09-26T10:00:00Z");

    /** Boundary values of the valid partition: one attempt, no correct answer, every answer correct. */
    @ParameterizedTest
    @CsvSource({"1, 0", "1, 1", "3, 3", "3, 2"})
    void acceptsCoherentCounts(int attempts, int correct) {
        assertThat(new HandStat(SituationKey.of("btn_open"), Stack.of(25), Hand.of("AA"), attempts, correct, AT)
                        .correct())
                .isEqualTo(correct);
        assertThat(new ProgressDay(LocalDate.parse("2026-09-26"), attempts, correct).attempts())
                .isEqualTo(attempts);
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "1, 2", "2, -1"})
    void rejectsImpossibleCounts(int attempts, int correct) {
        assertThatThrownBy(() ->
                        new HandStat(SituationKey.of("btn_open"), Stack.of(25), Hand.of("AA"), attempts, correct, AT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProgressDay(LocalDate.parse("2026-09-26"), attempts, correct))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
