package com.pedromorago.spintrainer.situation.domain;

import java.util.Objects;

/** Lo que hizo un jugador antes de la decisión del héroe (para dibujar la mesa del Quiz). */
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
