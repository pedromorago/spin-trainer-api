package com.pedromorago.spintrainer.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.internal.exception.FlywaySqlException;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

/** The native image can report why Flyway could not connect (ADR-0018). */
class FlywayHintsTest {

    @Test
    void registersEveryExceptionFlywayLooksUpByReflection() throws Exception {
        RuntimeHints hints = new RuntimeHints();

        new FlywayHints().registerHints(hints, getClass().getClassLoader());

        // The list Flyway itself walks: a Flyway upgrade that adds a class fails here, not in production.
        Method lookup = FlywaySqlException.class.getDeclaredMethod("getSpecificFlywaySqlExceptionClasses");
        lookup.setAccessible(true);
        List<Object> lookedUp = List.copyOf((List<?>) lookup.invoke(null));
        assertThat(lookedUp).isNotEmpty().containsExactlyInAnyOrderElementsOf(FlywayHints.LOOKED_UP_BY_REFLECTION);
        for (Class<?> type : FlywayHints.LOOKED_UP_BY_REFLECTION) {
            assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(type, "isFlywaySpecificVersionOf"))
                    .accepts(hints);
            assertThat(RuntimeHintsPredicates.reflection()
                            .onConstructorInvocation(type.getDeclaredConstructor(SQLException.class, DataSource.class)))
                    .accepts(hints);
        }
    }
}
