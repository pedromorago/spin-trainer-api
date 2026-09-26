package com.pedromorago.spintrainer.situation.domain;

import java.util.Objects;

/** What a player did before the hero's decision (to draw the Quiz table). */
public record PriorAction(Position position, Move move) {

    public enum Move {
        FOLD,
        LIMP,
        MIN_RAISE,
        RAISE,
        THREE_BET,
        CALL,
        SHOVE
    }

    public PriorAction {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(move, "move");
    }
}
