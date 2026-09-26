package com.pedromorago.spintrainer.quiz.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pedromorago.spintrainer.quiz.application.port.out.AttemptRepository.Position;
import com.pedromorago.spintrainer.quiz.domain.QuizAttempt;
import com.pedromorago.spintrainer.range.domain.RangeSource;
import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.DomainException;
import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AttemptCursorTest {

    @Test
    void roundTripsThePositionOfTheLastAttempt() {
        UUID id = UUID.randomUUID();
        Instant at = Instant.parse("2026-09-26T10:00:00.123Z");
        QuizAttempt last = new QuizAttempt(
                id,
                new UserId(UUID.randomUUID()),
                SituationKey.of("btn_open"),
                Stack.of(25),
                Hand.of("AA"),
                Action.FOLD,
                Action.FOLD,
                true,
                RangeSource.DEFAULT,
                1,
                at);

        String cursor = AttemptCursor.encode(last);

        assertThat(cursor).matches("[A-Za-z0-9_-]+");
        assertThat(AttemptCursor.decode(cursor)).isEqualTo(new Position(at, id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "no-es-base64!", "MTIz", "Zm9vfGJhcg"})
    void rejectsCursorsItDidNotIssue(String cursor) {
        assertThatThrownBy(() -> AttemptCursor.decode(cursor))
                .isInstanceOfSatisfying(
                        DomainException.class,
                        e -> assertThat(e.errors())
                                .extracting(DomainException.FieldError::field)
                                .containsExactly("cursor"));
    }
}
