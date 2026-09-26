package com.pedromorago.spintrainer.stats.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Las filas agregadas no pueden tener recuentos imposibles (una consulta mal escrita fallaría aquí, no en la web). */
class StatRowsTest {

    static final Instant AT = Instant.parse("2026-09-26T10:00:00Z");

    @Test
    void acceptsCoherentCounts() {
        assertThat(new HandStat(SituationKey.of("btn_open"), Stack.of(25), Hand.of("AA"), 3, 2, AT).correct())
                .isEqualTo(2);
        assertThat(new ProgressDay(LocalDate.parse("2026-09-26"), 1, 0).attempts())
                .isEqualTo(1);
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
