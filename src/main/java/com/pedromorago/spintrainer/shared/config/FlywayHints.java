package com.pedromorago.spintrainer.shared.config;

import java.util.List;
import org.flywaydb.core.internal.exception.sqlExceptions.FlywaySqlNoDriversForInteractiveAuthException;
import org.flywaydb.core.internal.exception.sqlExceptions.FlywaySqlNoIntegratedAuthException;
import org.flywaydb.core.internal.exception.sqlExceptions.FlywaySqlServerUntrustedCertificateSqlException;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Native image (ADR-0018): when Flyway cannot open a connection, it looks up its specific exceptions by reflection
 * ({@code FlywaySqlException.throwFlywayExceptionIfPossible}). Without these hints that lookup threw a
 * {@code NoSuchMethodException} that replaced the real cause, so a wrong password or user read as a missing method
 * (found on the first deploy to Render). {@code FlywayHintsTest} fails if Flyway looks up a class not listed here.
 */
@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(FlywayHints.class)
class FlywayHints implements RuntimeHintsRegistrar {

    static final List<Class<?>> LOOKED_UP_BY_REFLECTION = List.of(
            FlywaySqlServerUntrustedCertificateSqlException.class,
            FlywaySqlNoIntegratedAuthException.class,
            FlywaySqlNoDriversForInteractiveAuthException.class);

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        LOOKED_UP_BY_REFLECTION.forEach(type -> hints.reflection()
                .registerType(type, MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
    }
}
