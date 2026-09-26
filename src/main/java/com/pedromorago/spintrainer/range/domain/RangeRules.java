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
 * Reglas de los rangos. Es la única implementación en la API de "qué acción tiene esta mano", la misma que
 * {@code domain/range.js#actionFor} en la web: la corrección del Quiz en el servidor coincide con el feedback local.
 */
public final class RangeRules {

    private RangeRules() {}

    /** Acción efectiva: la explícita del rango o, si la mano no está, la implícita de la situación. */
    public static Action actionFor(Map<Hand, Action> hands, Hand hand, Situation situation) {
        return hands.getOrDefault(hand, situation.implicitAction());
    }

    /**
     * Valida un rango enviado por el usuario y lo normaliza: todas las manos canónicas y todas las acciones de la
     * situación (si no, un 400 con un error por mano), sin guardar las que tienen la acción implícita.
     */
    public static Map<Hand, Action> normalize(Map<String, Action> hands, Situation situation) {
        List<FieldError> errors = new ArrayList<>();
        Map<Hand, Action> normalized = new TreeMap<>();
        hands.forEach((code, action) -> {
            String field = "hands." + code;
            if (!Hand.isCanonical(code)) {
                errors.add(new FieldError(field, "mano no válida"));
            } else if (action == null || !situation.allows(action)) {
                errors.add(new FieldError(
                        field,
                        "acción " + (action == null ? "vacía" : action.code()) + " no permitida en "
                                + situation.key()));
            } else if (action != situation.implicitAction()) {
                normalized.put(Hand.of(code), action);
            }
        });
        if (!errors.isEmpty()) {
            throw DomainException.validation(errors.size() + " entradas no válidas en hands", errors);
        }
        return normalized;
    }
}
