package com.pedromorago.spintrainer.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/** Equivalence partitioning: one class per kind of constraint the generated code uses, plus an unknown one. */
class ConstraintMessagesTest {

    record Sample(
            @NotNull String required,
            @Pattern(regexp = "^[a-z]+$") String key,
            @Min(1) @Max(200) Integer limit,

            @DecimalMin("1") @DecimalMax(value = "100", inclusive = false)
            BigDecimal stack,

            @DecimalMin(value = "0", inclusive = false) BigDecimal positive,
            @Size(max = 3) String shortText,
            @Size(min = 2) List<String> atLeastTwo,
            @Size(min = 1, max = 2) List<String> oneOrTwo,
            @Email String email) {}

    static final ValidatorFactory FACTORY = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory();

    @AfterAll
    static void close() {
        FACTORY.close();
    }

    @Test
    void everyConstraintOfTheSpecHasItsEnglishMessage() {
        Map<String, String> messages = messages(new Sample(
                null,
                "BTN",
                0,
                new BigDecimal("100"),
                BigDecimal.ZERO,
                "abcd",
                List.of("x"),
                List.of(),
                "no-es-un-email"));

        assertThat(messages)
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        "required", "required",
                        "key", "invalid format",
                        "limit", "must be ≥ 1",
                        "stack", "must be < 100",
                        "positive", "must be > 0",
                        "shortText", "size must be at most 3",
                        "atLeastTwo", "size must be at least 2",
                        "oneOrTwo", "size must be between 1 and 2",
                        "email", ConstraintMessages.FALLBACK));
    }

    @Test
    void theUpperBoundsToo() {
        Map<String, String> messages =
                messages(new Sample("ok", "btn", 201, new BigDecimal("0.5"), BigDecimal.ONE, "abc", null, null, null));

        assertThat(messages)
                .containsExactlyInAnyOrderEntriesOf(Map.of("limit", "must be ≤ 200", "stack", "must be ≥ 1"));
    }

    @Test
    void doNotDependOnTheJvmLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            assertThat(messages(new Sample(null, "btn", 1, BigDecimal.ONE, BigDecimal.ONE, "", null, null, null)))
                    .containsExactly(Map.entry("required", "required"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    private static Map<String, String> messages(Sample sample) {
        Validator validator = FACTORY.getValidator();
        Map<String, String> messages = new TreeMap<>();
        for (ConstraintViolation<Sample> violation : validator.validate(sample)) {
            messages.put(
                    violation.getPropertyPath().toString(), ConstraintMessages.of(violation.getConstraintDescriptor()));
        }
        return messages;
    }
}
