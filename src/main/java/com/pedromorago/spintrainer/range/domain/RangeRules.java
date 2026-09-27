package com.pedromorago.spintrainer.range.domain;

import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.DomainException;
import com.pedromorago.spintrainer.shared.kernel.DomainException.FieldError;
import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.situation.domain.Situation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Range rules. It is the only implementation in the API of "which action does this hand have", the same as
 * {@code domain/range.js#actionFor} in the web: the Quiz grading on the server matches the local feedback.
 */
public final class RangeRules {

    private RangeRules() {}

    /** Effective action: the explicit one of the range or, if the hand is absent, the implicit one of the situation. */
    public static Action actionFor(Map<Hand, Action> hands, Hand hand, Situation situation) {
        return hands.getOrDefault(hand, situation.implicitAction());
    }

    /**
     * Validates a range sent by the user and normalizes it: every hand canonical and every action from the situation
     * (otherwise, a 400 with one error per hand), without storing those with the implicit action.
     */
    public static Map<Hand, Action> normalize(Map<String, Action> hands, Situation situation) {
        List<FieldError> errors = new ArrayList<>();
        Map<Hand, Action> normalized = new TreeMap<>();
        hands.forEach((code, action) -> {
            String field = "hands." + code;
            if (!Hand.isCanonical(code)) {
                errors.add(new FieldError(field, "invalid hand"));
            } else if (action == null || !situation.allows(action)) {
                errors.add(new FieldError(
                        field,
                        (action == null ? "empty action" : "action " + action.code()) + " not allowed in "
                                + situation.key()));
            } else if (action != situation.implicitAction()) {
                normalized.put(Hand.of(code), action);
            }
        });
        if (!errors.isEmpty()) {
            throw DomainException.validation(errors.size() + " invalid entries in hands", errors);
        }
        return normalized;
    }
}
