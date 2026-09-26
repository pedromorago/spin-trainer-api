package com.pedromorago.spintrainer.testsupport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Test Postgres (Testcontainers) prepared like a real environment: the same {@code db/bootstrap/bootstrap.sql} creates
 * the {@code spin_migrator} and {@code spin_app} roles, and the application migrates with one and connects with the
 * other. Same major version as Supabase.
 */
public final class PostgresTestDatabase {

    public static final String IMAGE = "postgres:17-alpine";
    public static final String MIGRATOR_USER = "spin_migrator";
    public static final String MIGRATOR_PASSWORD = "migrator-test";
    public static final String APP_USER = "spin_app";
    public static final String APP_PASSWORD = "app-test";

    private static PostgresTestDatabase shared;

    private final PostgreSQLContainer container;

    private PostgresTestDatabase() {
        container = new PostgreSQLContainer(IMAGE);
        container.start();
        bootstrap();
    }

    /** Database shared by the whole test JVM (Testcontainers removes it on exit). */
    public static synchronized PostgresTestDatabase shared() {
        if (shared == null) {
            shared = new PostgresTestDatabase();
        }
        return shared;
    }

    /** New database only for whoever requests it (e.g. applying the migrations from scratch); stop it when done. */
    public static PostgresTestDatabase fresh() {
        return new PostgresTestDatabase();
    }

    public void stop() {
        container.stop();
    }

    public String jdbcUrl() {
        return container.getJdbcUrl();
    }

    /** Spring properties to connect the API: as spin_app, and Flyway as spin_migrator. */
    public Map<String, String> springProperties() {
        return Map.of(
                "spring.datasource.url", jdbcUrl(),
                "spring.datasource.username", APP_USER,
                "spring.datasource.password", APP_PASSWORD,
                "spring.flyway.url", jdbcUrl(),
                "spring.flyway.user", MIGRATOR_USER,
                "spring.flyway.password", MIGRATOR_PASSWORD);
    }

    /** Administrator connection, to inspect or prepare data that the API cannot write. */
    public Connection adminConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl(), container.getUsername(), container.getPassword());
    }

    public Connection connectionAs(String user, String password) throws SQLException {
        return DriverManager.getConnection(jdbcUrl(), user, password);
    }

    private void bootstrap() {
        String script = readClasspath("/db/bootstrap/bootstrap.sql")
                .replace(":'migrator_password'", "'" + MIGRATOR_PASSWORD + "'")
                .replace(":'app_password'", "'" + APP_PASSWORD + "'");
        try (Connection connection = adminConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(script);
        } catch (SQLException e) {
            throw new IllegalStateException("db/bootstrap/bootstrap.sql failed", e);
        }
    }

    private static String readClasspath(String path) {
        try (InputStream in = PostgresTestDatabase.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Not on the classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
