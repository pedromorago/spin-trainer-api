package com.pedromorago.spintrainer.range;

import static org.assertj.core.api.Assertions.assertThat;

import com.pedromorago.spintrainer.testsupport.PostgresTestDatabase;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Las migraciones aplicadas desde cero dejan exactamente los rangos de {@code reference-ranges.json}, el fichero que
 * copian la web (mock) y spin-trainer-qa (oráculos): si cambia uno sin el otro, falla. Base de datos propia, sin la
 * aplicación: el resto de los IT modifican los rangos de referencia de la compartida.
 */
class ReferenceSeedIT {

    static PostgresTestDatabase database;

    @BeforeAll
    static void migrateFromScratch() {
        database = PostgresTestDatabase.fresh();
        // La misma configuración que spring.flyway en application.yaml.
        Flyway.configure()
                .dataSource(
                        database.jdbcUrl(), PostgresTestDatabase.MIGRATOR_USER, PostgresTestDatabase.MIGRATOR_PASSWORD)
                .schemas("app")
                .placeholders(Map.of("app_role", PostgresTestDatabase.APP_USER))
                .load()
                .migrate();
    }

    @AfterAll
    static void stop() {
        database.stop();
    }

    @Test
    void theSeedIsTheReferenceFile() throws Exception {
        Map<String, Map<String, String>> expected = new TreeMap<>();
        JsonNode file = JsonMapper.builder()
                .build()
                .readTree(Files.readString(Path.of("reference-ranges.json"), StandardCharsets.UTF_8));
        for (JsonNode range : file.get("ranges")) {
            Map<String, String> hands = new TreeMap<>();
            range.get("hands")
                    .properties()
                    .forEach(hand -> hands.put(hand.getKey(), hand.getValue().asString()));
            expected.put(
                    spot(range.get("situation").asString(), range.get("stack").decimalValue()), hands);
        }

        Map<String, Map<String, String>> seeded = new TreeMap<>();
        query("SELECT situation, stack FROM app.default_range", row -> seeded.put(spot(row), new TreeMap<>()));
        query(
                "SELECT situation, stack, hand, action FROM app.default_range_hand",
                row -> seeded.get(spot(row)).put(row.getString("hand"), row.getString("action")));

        assertThat(seeded).hasSize(73).isEqualTo(expected);
    }

    @Test
    void everyReferenceRangeIsVersionOne() throws Exception {
        List<String> versions = new ArrayList<>();
        query("SELECT DISTINCT version FROM app.default_range", row -> versions.add(row.getString("version")));

        assertThat(versions).containsExactly("1");
    }

    @Test
    void everySpotOfTheCatalogHasAReferenceRange() throws Exception {
        List<String> missing = new ArrayList<>();
        query("""
                SELECT s.situation, s.stack FROM app.situation_stack s
                LEFT JOIN app.default_range r ON r.situation = s.situation AND r.stack = s.stack
                WHERE r.situation IS NULL""", row -> missing.add(spot(row)));

        assertThat(missing).isEmpty();
    }

    @Test
    void theCatalogHasTheActionsOfThePdfLegends() throws Exception {
        Map<String, List<String>> actions = new LinkedHashMap<>();
        query(
                "SELECT situation, action FROM app.situation_action WHERE situation IN ('sb_open', 'hu_sb_open') "
                        + "ORDER BY situation, seq",
                row -> actions.computeIfAbsent(row.getString("situation"), s -> new ArrayList<>())
                        .add(row.getString("action")));

        assertThat(actions)
                .containsEntry(
                        "sb_open", List.of("MR_4B_C", "MR_C_C", "MR_C_F", "MR_F_F", "L_C_F", "L_F", "ALLIN", "FOLD"))
                .containsEntry(
                        "hu_sb_open",
                        List.of(
                                "MR_4B_C", "MR_C_C", "MR_C_F", "MR_F_F", "L_PUSH", "L_C_C", "L_C_F", "L_F", "ALLIN",
                                "FOLD"));
    }

    interface Row {
        void accept(ResultSet row) throws SQLException;
    }

    static void query(String sql, Row consumer) throws SQLException {
        try (Connection admin = database.adminConnection();
                Statement statement = admin.createStatement();
                ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                consumer.accept(rows);
            }
        }
    }

    static String spot(ResultSet row) throws SQLException {
        return spot(row.getString("situation"), row.getBigDecimal("stack"));
    }

    static String spot(String situation, BigDecimal stack) {
        return situation + "@" + stack.stripTrailingZeros().toPlainString();
    }
}
