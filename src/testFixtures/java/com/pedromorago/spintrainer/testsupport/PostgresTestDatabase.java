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
 * Postgres de pruebas (Testcontainers) preparado como un entorno real: el mismo {@code db/bootstrap/bootstrap.sql}
 * crea los roles {@code spin_migrator} y {@code spin_app}, y la aplicación migra con uno y se conecta con el otro.
 * Misma versión mayor que Supabase.
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

    /** Base de datos compartida por toda la JVM de tests (Testcontainers la elimina al salir). */
    public static synchronized PostgresTestDatabase shared() {
        if (shared == null) {
            shared = new PostgresTestDatabase();
        }
        return shared;
    }

    /** Base de datos nueva y solo para quien la pide (p. ej. aplicar las migraciones desde cero); pararla al acabar. */
    public static PostgresTestDatabase fresh() {
        return new PostgresTestDatabase();
    }

    public void stop() {
        container.stop();
    }

    public String jdbcUrl() {
        return container.getJdbcUrl();
    }

    /** Propiedades de Spring para conectar la API: como spin_app, y Flyway como spin_migrator. */
    public Map<String, String> springProperties() {
        return Map.of(
                "spring.datasource.url", jdbcUrl(),
                "spring.datasource.username", APP_USER,
                "spring.datasource.password", APP_PASSWORD,
                "spring.flyway.url", jdbcUrl(),
                "spring.flyway.user", MIGRATOR_USER,
                "spring.flyway.password", MIGRATOR_PASSWORD);
    }

    /** Conexión de administrador, para inspeccionar o preparar datos que la API no puede escribir. */
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
            throw new IllegalStateException("Falló db/bootstrap/bootstrap.sql", e);
        }
    }

    private static String readClasspath(String path) {
        try (InputStream in = PostgresTestDatabase.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("No está en el classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
