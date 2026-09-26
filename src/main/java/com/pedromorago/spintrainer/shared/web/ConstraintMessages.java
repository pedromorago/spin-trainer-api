package com.pedromorago.spintrainer.shared.web;

import jakarta.validation.metadata.ConstraintDescriptor;
import java.util.Map;

/**
 * Messages of the constraints generated from the spec (required, pattern, minimum...), in Spanish like every other API
 * error. Hibernate Validator's own messages follow the JVM's locale: English in the container and in CI, Spanish on a
 * Spanish Windows machine.
 */
final class ConstraintMessages {

    static final String FALLBACK = "valor no válido";

    private ConstraintMessages() {}

    static String of(ConstraintDescriptor<?> constraint) {
        Map<String, Object> attributes = constraint.getAttributes();
        boolean exclusive = Boolean.FALSE.equals(attributes.get("inclusive"));
        return switch (constraint.getAnnotation().annotationType().getSimpleName()) {
            case "NotNull" -> "obligatorio";
            case "Pattern" -> "formato no válido";
            case "Min", "DecimalMin" -> (exclusive ? "debe ser > " : "debe ser ≥ ") + attributes.get("value");
            case "Max", "DecimalMax" -> (exclusive ? "debe ser < " : "debe ser ≤ ") + attributes.get("value");
            case "Size" -> size((Integer) attributes.get("min"), (Integer) attributes.get("max"));
            default -> FALLBACK;
        };
    }

    private static String size(int min, int max) {
        if (min == 0) {
            return "tamaño máximo " + max;
        }
        return max == Integer.MAX_VALUE ? "tamaño mínimo " + min : "tamaño entre " + min + " y " + max;
    }
}
