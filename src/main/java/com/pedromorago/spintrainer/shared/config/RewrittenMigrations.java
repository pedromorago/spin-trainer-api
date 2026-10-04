package com.pedromorago.spintrainer.shared.config;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ADR-0024: migrations V2, V5, V7 and V8 were rewritten to take a third party's tables out of the repository. A
 * database migrated with their first contents (production) keeps those contents' checksums, which Flyway's validation
 * would reject. Recognised by them, it is repaired once before migrating (its history takes the checksums of the
 * files), and V9 then replaces its reference ranges. A database without those checksums is migrated as always, so any
 * other mismatch still stops the start.
 */
@Configuration(proxyBeanMethods = false)
class RewrittenMigrations {

    /** Version → the checksum Flyway recorded for its first contents. */
    static final Map<String, Integer> FIRST_CHECKSUMS =
            Map.of("2", -1204866423, "5", -1933973688, "7", -567980126, "8", 506071727);

    @Bean
    FlywayMigrationStrategy realignRewrittenMigrationsThenMigrate() {
        return flyway -> {
            if (hasFirstContents(flyway.info().applied())) {
                flyway.repair();
            }
            flyway.migrate();
        };
    }

    static boolean hasFirstContents(MigrationInfo[] applied) {
        return Arrays.stream(applied)
                .filter(migration -> migration.getVersion() != null)
                .anyMatch(migration -> Objects.equals(
                        FIRST_CHECKSUMS.get(migration.getVersion().getVersion()), migration.getChecksum()));
    }
}
