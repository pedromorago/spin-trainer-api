package com.pedromorago.spintrainer.shared.web;

import com.pedromorago.spintrainer.shared.kernel.DomainException.FieldError;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Native image (ADR-0018): Jackson writes a Problem's {@code errors} by reflection, and Spring AOT cannot see those
 * records inside the properties map. Without this hint every 400 with field errors turned into a 500 in the native image
 * (found by the spin-trainer-qa suite).
 */
class ProblemDetailsHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        new BindingReflectionHintsRegistrar().registerReflectionHints(hints.reflection(), FieldError.class);
    }
}
