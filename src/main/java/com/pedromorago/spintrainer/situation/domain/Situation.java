package com.pedromorago.spintrainer.situation.domain;

import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Spin & Go situation: who decides (hero), what happened before, with which stacks and among which actions. The hands
 * that a range does not list have the implicit action: FOLD, or CHECK if FOLD is not possible (the BB facing a limp).
 */
public record Situation(
        SituationKey key,
        String label,
        Format format,
        Position hero,
        List<PriorAction> priorActions,
        List<Stack> stacks,
        List<Action> actions,
        Optional<String> notes) {

    public Situation {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(hero, "hero");
        priorActions = List.copyOf(priorActions);
        stacks = List.copyOf(stacks);
        actions = List.copyOf(actions);
        Objects.requireNonNull(notes, "notes");
        if (stacks.isEmpty()) {
            throw new IllegalArgumentException(key + ": sin stacks");
        }
        if (actions.size() < 2) {
            throw new IllegalArgumentException(key + ": necesita al menos dos acciones");
        }
    }

    public boolean hasStack(Stack stack) {
        return stacks.contains(stack);
    }

    public boolean allows(Action action) {
        return actions.contains(action);
    }

    /** Action of the hands that the range does not list (same rule as {@code fallbackAction} in the web). */
    public Action implicitAction() {
        return allows(Action.FOLD) ? Action.FOLD : Action.CHECK;
    }
}
