package com.pedromorago.spintrainer.situation.domain;

import static com.pedromorago.spintrainer.situation.SituationFixtures.bbVsSbLimp;
import static com.pedromorago.spintrainer.situation.SituationFixtures.btnOpen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SituationTest {

    @Test
    void theImplicitActionIsFoldWhenFoldingIsPossible() {
        assertThat(btnOpen().implicitAction()).isEqualTo(Action.FOLD);
    }

    @Test
    void theImplicitActionIsCheckWhenFoldIsNotAnOption() {
        assertThat(bbVsSbLimp().implicitAction()).isEqualTo(Action.CHECK);
    }

    @Test
    void knowsItsStacksAndActions() {
        Situation situation = btnOpen();

        assertThat(situation.hasStack(Stack.of(12))).isTrue();
        assertThat(situation.hasStack(Stack.of(12.5))).isFalse();
        assertThat(situation.allows(Action.MR_4B_C)).isTrue();
        assertThat(situation.allows(Action.CHECK)).isFalse();
    }

    @Test
    void needsStacksAndAtLeastTwoActions() {
        assertThatThrownBy(() -> new Situation(
                        SituationKey.of("x"),
                        "X",
                        Format.HEADS_UP,
                        Position.SB,
                        List.of(),
                        List.of(),
                        List.of(Action.CALL, Action.FOLD),
                        Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Situation(
                        SituationKey.of("x"),
                        "X",
                        Format.HEADS_UP,
                        Position.SB,
                        List.of(),
                        List.of(Stack.of(25)),
                        List.of(Action.FOLD),
                        Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
