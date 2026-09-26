package com.pedromorago.spintrainer.quiz.domain;

import static com.pedromorago.spintrainer.situation.SituationFixtures.bbVsSbLimp;
import static com.pedromorago.spintrainer.situation.SituationFixtures.btnOpen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.range.domain.RangeSource;
import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.DomainException;
import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuizAttemptTest {

    static final Instant AT = Instant.parse("2026-09-26T10:00:00Z");
    final UserId user = new UserId(UUID.randomUUID());
    final Range btnOpen25 = new Range(
            SituationKey.of("btn_open"),
            Stack.of(25),
            Map.of(Hand.of("AA"), Action.MR_4B_C),
            RangeSource.USER,
            4,
            Optional.of(AT));

    QuizAttempt grade(String hand, Action given) {
        return QuizAttempt.grade(UUID.randomUUID(), user, btnOpen(), Stack.of(25), Hand.of(hand), given, btnOpen25, AT);
    }

    @Test
    void aMatchingAnswerIsCorrectAndRemembersTheRangeItCameFrom() {
        QuizAttempt attempt = grade("AA", Action.MR_4B_C);

        assertThat(attempt.expected()).isEqualTo(Action.MR_4B_C);
        assertThat(attempt.correct()).isTrue();
        assertThat(attempt.rangeSource()).isEqualTo(RangeSource.USER);
        assertThat(attempt.rangeVersion()).isEqualTo(4);
    }

    @Test
    void aHandOutsideTheRangeExpectsTheImplicitAction() {
        assertThat(grade("72o", Action.FOLD).correct()).isTrue();
        assertThat(grade("72o", Action.ALLIN))
                .extracting(QuizAttempt::expected, QuizAttempt::correct)
                .containsExactly(Action.FOLD, false);
    }

    @Test
    void whereFoldIsNotPossibleTheImplicitActionIsCheck() {
        Range empty = new Range(
                SituationKey.of("bb_vs_sb_limp"), Stack.of(10), Map.of(), RangeSource.DEFAULT, 1, Optional.empty());

        QuizAttempt attempt = QuizAttempt.grade(
                UUID.randomUUID(), user, bbVsSbLimp(), Stack.of(10), Hand.of("72o"), Action.CHECK, empty, AT);

        assertThat(attempt.expected()).isEqualTo(Action.CHECK);
        assertThat(attempt.correct()).isTrue();
    }

    @Test
    void anActionThatDoesNotExistInTheSituationIsInvalid() {
        assertThatThrownBy(() -> grade("AA", Action.CHECK)).isInstanceOfSatisfying(DomainException.class, e -> {
            assertThat(e.kind()).isEqualTo(DomainException.Kind.VALIDATION);
            assertThat(e.errors()).extracting(DomainException.FieldError::field).containsExactly("given");
        });
    }

    @Test
    void correctnessCannotContradictTheActions() {
        assertThatThrownBy(() -> new QuizAttempt(
                        UUID.randomUUID(),
                        user,
                        SituationKey.of("btn_open"),
                        Stack.of(25),
                        Hand.of("AA"),
                        Action.FOLD,
                        Action.MR_4B_C,
                        true,
                        RangeSource.DEFAULT,
                        1,
                        AT))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
