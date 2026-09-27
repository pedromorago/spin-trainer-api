package com.pedromorago.spintrainer.shared.web;

import jakarta.validation.metadata.ConstraintDescriptor;
import java.util.Map;

/**
 * Messages of the constraints generated from the spec (required, pattern, minimum...), in English like every other API
 * error (ADR-0021). Hibernate Validator's own messages follow the JVM's locale (Spanish on a Spanish Windows machine),
 * so they are not used.
 */
final class ConstraintMessages {

    static final String FALLBACK = "invalid value";

    private ConstraintMessages() {}

    static String of(ConstraintDescriptor<?> constraint) {
        Map<String, Object> attributes = constraint.getAttributes();
        boolean exclusive = Boolean.FALSE.equals(attributes.get("inclusive"));
        return switch (constraint.getAnnotation().annotationType().getSimpleName()) {
            case "NotNull" -> "required";
            case "Pattern" -> "invalid format";
            case "Min", "DecimalMin" -> (exclusive ? "must be > " : "must be ≥ ") + attributes.get("value");
            case "Max", "DecimalMax" -> (exclusive ? "must be < " : "must be ≤ ") + attributes.get("value");
            case "Size" -> size((Integer) attributes.get("min"), (Integer) attributes.get("max"));
            default -> FALLBACK;
        };
    }

    private static String size(int min, int max) {
        if (min == 0) {
            return "size must be at most " + max;
        }
        return max == Integer.MAX_VALUE
                ? "size must be at least " + min
                : "size must be between " + min + " and " + max;
    }
}
