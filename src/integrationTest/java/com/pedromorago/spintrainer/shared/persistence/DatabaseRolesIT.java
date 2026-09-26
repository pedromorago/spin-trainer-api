package com.pedromorago.spintrainer.shared.persistence;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pedromorago.spintrainer.support.ApiIntegrationTest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Mínimos privilegios (ADR-0015): la API se conecta como spin_app y solo tiene los permisos que declaran las
 * migraciones. Si una migración añade una tabla sin decidir sus permisos, este test falla.
 */
class DatabaseRolesIT extends ApiIntegrationTest {

    static final Set<String> READ = Set.of("SELECT");
    static final Set<String> READ_WRITE = Set.of("SELECT", "INSERT", "UPDATE", "DELETE");

    /** Permisos esperados de spin_app en cada tabla del esquema app. */
    static final Map<String, Set<String>> EXPECTED = Map.ofEntries(
            entry("flyway_schema_history", Set.of()),
            entry("hand", READ),
            entry("situation", READ),
            entry("situation_prior_action", READ),
            entry("situation_stack", READ),
            entry("situation_action", READ),
            entry("default_range", READ),
            entry("default_range_hand", READ),
            entry("user_range", READ_WRITE),
            entry("user_range_hand", READ_WRITE));

    @Autowired
    JdbcClient jdbc;

    @Test
    void theApiConnectsWithTheRuntimeRole() {
        assertThat(jdbc.sql("SELECT current_user").query(String.class).single()).isEqualTo("spin_app");
    }

    @Test
    void theRuntimeRoleHasExactlyTheDeclaredPrivileges() throws SQLException {
        // Como administrador: information_schema solo le enseña a spin_app las tablas en las que ya tiene permisos.
        Map<String, Set<String>> actual = new TreeMap<>();
        try (Connection admin = DB.adminConnection();
                PreparedStatement query = admin.prepareStatement("""
                        SELECT c.relname AS table_name, p.privilege,
                               has_table_privilege('spin_app', c.oid, p.privilege) AS granted
                        FROM pg_class c
                        JOIN pg_namespace n ON n.oid = c.relnamespace
                        CROSS JOIN unnest(ARRAY['SELECT', 'INSERT', 'UPDATE', 'DELETE', 'TRUNCATE', 'REFERENCES',
                                                'TRIGGER']) AS p(privilege)
                        WHERE n.nspname = 'app' AND c.relkind = 'r'""");
                ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                Set<String> privileges = actual.computeIfAbsent(rows.getString("table_name"), table -> new TreeSet<>());
                if (rows.getBoolean("granted")) {
                    privileges.add(rows.getString("privilege"));
                }
            }
        }

        assertThat(actual).isEqualTo(new TreeMap<>(EXPECTED));
    }

    @Test
    void theRuntimeRoleCannotChangeTheSchemaOrTheCatalog() {
        assertThatThrownBy(() -> jdbc.sql("CREATE TABLE app.intruder (id int)").update())
                .rootCause()
                .hasMessageContaining("permission denied for schema app");
        assertThatThrownBy(
                        () -> jdbc.sql("UPDATE app.situation SET label = 'x'").update())
                .rootCause()
                .hasMessageContaining("permission denied for table situation");
    }

    @Test
    void theHandTableHoldsTheCanonical169Hands() {
        assertThat(jdbc.sql("SELECT count(*) FROM app.hand")
                        .query(Integer.class)
                        .single())
                .isEqualTo(169);
        assertThat(jdbc.sql("SELECT code FROM app.hand WHERE code IN ('AA', 'AKs', 'AKo', '32o', 'KAs', 'AAs')")
                        .query(String.class)
                        .set())
                .containsExactlyInAnyOrder("AA", "AKs", "AKo", "32o");
    }
}
