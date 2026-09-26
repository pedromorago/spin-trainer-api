package com.pedromorago.spintrainer.situation;

import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.situation.domain.Format;
import com.pedromorago.spintrainer.situation.domain.Position;
import com.pedromorago.spintrainer.situation.domain.PriorAction;
import com.pedromorago.spintrainer.situation.domain.Situation;
import java.util.List;
import java.util.Optional;

/** Situaciones del catálogo real para los tests unitarios. */
public final class SituationFixtures {

    private SituationFixtures() {}

    public static Situation btnOpen() {
        return new Situation(
                SituationKey.of("btn_open"),
                "BTN Open",
                Format.THREE_MAX,
                Position.BTN,
                List.of(),
                List.of(Stack.of(25), Stack.of(20), Stack.of(15), Stack.of(12), Stack.of(10), Stack.of(8)),
                List.of(
                        Action.MR_4B_C,
                        Action.MR_C_C,
                        Action.MR_C_F,
                        Action.MR_F_F,
                        Action.L_C_C,
                        Action.L_C_F,
                        Action.ALLIN,
                        Action.FOLD),
                Optional.empty());
    }

    /** La BB ante un limp no puede foldear: la acción implícita es CHECK. */
    public static Situation bbVsSbLimp() {
        return new Situation(
                SituationKey.of("bb_vs_sb_limp"),
                "BB vs SB Limp",
                Format.THREE_MAX,
                Position.BB,
                List.of(
                        new PriorAction(Position.BTN, PriorAction.Move.FOLD),
                        new PriorAction(Position.SB, PriorAction.Move.LIMP)),
                List.of(Stack.of(25), Stack.of(20), Stack.of(15), Stack.of(10)),
                List.of(Action.ALLIN, Action.ISO_C, Action.CHECK),
                Optional.empty());
    }
}
