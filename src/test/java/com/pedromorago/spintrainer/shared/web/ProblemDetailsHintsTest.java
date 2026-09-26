package com.pedromorago.spintrainer.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.pedromorago.spintrainer.shared.kernel.DomainException.FieldError;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

/** The native image can serialize the field errors of a Problem (ADR-0018). */
class ProblemDetailsHintsTest {

    @Test
    void registersTheFieldErrorsForJackson() {
        RuntimeHints hints = new RuntimeHints();

        new ProblemDetailsHints().registerHints(hints, getClass().getClassLoader());

        assertThat(RuntimeHintsPredicates.reflection().onType(FieldError.class)).accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(FieldError.class, "field"))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(FieldError.class, "message"))
                .accepts(hints);
    }
}
