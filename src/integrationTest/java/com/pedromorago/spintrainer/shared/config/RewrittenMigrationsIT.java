package com.pedromorago.spintrainer.shared.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pedromorago.spintrainer.testsupport.PostgresTestDatabase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;

/**
 * ADR-0024: production was migrated with the first contents of V2, V5, V7 and V8. Played here by a database migrated
 * up to V8 whose history carries those contents' checksums, with a third-party range and note in it: the application's
 * migration strategy must realign it and apply V9; any other database keeps Flyway's validation.
 */
class RewrittenMigrationsIT {

    final FlywayMigrationStrategy strategy = new RewrittenMigrations().realignRewrittenMigrationsThenMigrate();
    PostgresTestDatabase database;

    @BeforeEach
    void start() {
        database = PostgresTestDatabase.fresh();
    }

    @AfterEach
    void stop() {
        database.stop();
    }

    @Test
    void aDatabaseMigratedWithTheFirstContentsIsRealignedAndGetsTheExamples() throws SQLException {
        flyway().target("8").load().migrate();
        StringBuilder firstContents = new StringBuilder();
        RewrittenMigrations.FIRST_CHECKSUMS.forEach((version, checksum) ->
                firstContents.append("UPDATE app.flyway_schema_history SET checksum = %d WHERE version = '%s';"
                        .formatted(checksum, version)));
        admin(firstContents + """
                INSERT INTO app.default_range (situation, stack, version) VALUES ('btn_open', 25, 1);
                INSERT INTO app.default_range_hand (situation, stack, hand, action)
                    VALUES ('btn_open', 25, '72o', 'MR_4B_C');
                UPDATE app.situation SET notes = 'A third party''s advice' WHERE key = 'sb_open';""");
        Flyway flyway = flyway().load();

        assertThatThrownBy(flyway::validate)
                .as("without the strategy, Flyway refuses to start")
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("checksum mismatch");

        strategy.migrate(flyway);

        flyway.validate();
        assertThat(flyway.info().applied())
                .extracting(migration -> migration.getVersion() == null
                        ? null
                        : migration.getVersion().getVersion())
                .contains("9");
        assertThat(RewrittenMigrations.hasFirstContents(flyway.info().applied()))
                .isFalse();
        assertThat(single("SELECT count(*) FROM app.default_range WHERE version = 2"))
                .isEqualTo("80");
        assertThat(single("SELECT count(*) FROM app.default_range WHERE version <> 2"))
                .isEqualTo("0");
        assertThat(single("""
                        SELECT count(*) FROM app.default_range_hand
                        WHERE situation = 'btn_open' AND stack = 25 AND hand = '72o'"""))
                .as("the third party's range is gone: 72o folds in the example")
                .isEqualTo("0");
        assertThat(single("SELECT count(*) FROM app.situation WHERE notes IS NOT NULL"))
                .isEqualTo("0");
    }

    @Test
    void anyOtherMismatchStillStopsTheStart() throws SQLException {
        flyway().load().migrate();
        admin("UPDATE app.flyway_schema_history SET checksum = 1 WHERE version = '3'");
        Flyway flyway = flyway().load();

        assertThatThrownBy(() -> strategy.migrate(flyway))
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("checksum mismatch");
        assertThat(single("SELECT checksum FROM app.flyway_schema_history WHERE version = '3'"))
                .as("not repaired")
                .isEqualTo("1");
    }

    @Test
    void aDatabaseFromScratchIsMigratedAsAlways() throws SQLException {
        Flyway flyway = flyway().load();

        strategy.migrate(flyway);

        MigrateResult again = flyway.migrate();
        assertThat(again.migrationsExecuted).isZero();
        assertThat(single("SELECT count(*) FROM app.default_range")).isEqualTo("80");
    }

    /** The same configuration as spring.flyway in application.yaml. */
    private org.flywaydb.core.api.configuration.FluentConfiguration flyway() {
        return Flyway.configure()
                .dataSource(
                        database.jdbcUrl(), PostgresTestDatabase.MIGRATOR_USER, PostgresTestDatabase.MIGRATOR_PASSWORD)
                .schemas("app")
                .placeholders(Map.of("app_role", PostgresTestDatabase.APP_USER));
    }

    private void admin(String sql) throws SQLException {
        try (Connection admin = database.adminConnection();
                Statement statement = admin.createStatement()) {
            statement.execute(sql);
        }
    }

    private String single(String sql) throws SQLException {
        try (Connection admin = database.adminConnection();
                Statement statement = admin.createStatement();
                ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getString(1);
        }
    }
}
