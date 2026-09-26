package com.pedromorago.spintrainer.shared.kernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class StackTest {

    @ParameterizedTest
    @CsvSource({"25, 25", "25.0, 25", "25.00, 25", "12.5, 12.5", "12.50, 12.5", "1, 1", "100, 100", "100.0, 100"})
    void normalizesToTheCanonicalForm(String input, String canonical) {
        Stack stack = Stack.of(new BigDecimal(input));

        assertThat(stack.bigBlinds().toString()).isEqualTo(canonical);
        assertThat(stack).isEqualTo(Stack.of(new BigDecimal(canonical)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12.3", "12.25", "0.5", "0", "-5", "100.5", "101"})
    void rejectsStacksOutsideTheGrid(String input) {
        assertThatThrownBy(() -> Stack.of(new BigDecimal(input)))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> {
                    DomainException domain = (DomainException) e;
                    assertThat(domain.kind()).isEqualTo(DomainException.Kind.VALIDATION);
                    assertThat(domain.errors())
                            .extracting(DomainException.FieldError::field)
                            .containsExactly("stack");
                });
    }

    @ParameterizedTest
    @CsvSource({"8, 12.5", "12.5, 25", "10, 12"})
    void ordersByBigBlinds(double smaller, double bigger) {
        assertThat(Stack.of(smaller)).isLessThan(Stack.of(bigger));
    }
}
