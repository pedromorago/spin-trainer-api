package com.pedromorago.spintrainer.support;

import com.pedromorago.spintrainer.testsupport.PostgresTestDatabase;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * Datos que la API no puede escribir (spin_app solo lee los rangos de referencia): se insertan como administrador,
 * igual que lo haría una migración de seed. Idempotente: los tests comparten base de datos.
 */
public final class TestData {

    private TestData() {}

    public static void defaultRange(String situation, double stack, int version, Map<String, String> hands) {
        try (Connection admin = PostgresTestDatabase.shared().adminConnection()) {
            admin.setAutoCommit(false);
            try (PreparedStatement delete =
                    admin.prepareStatement("DELETE FROM app.default_range WHERE situation = ? AND stack = ?")) {
                delete.setString(1, situation);
                delete.setBigDecimal(2, BigDecimal.valueOf(stack));
                delete.executeUpdate();
            }
            try (PreparedStatement insert = admin.prepareStatement(
                    "INSERT INTO app.default_range (situation, stack, version) VALUES (?, ?, ?)")) {
                insert.setString(1, situation);
                insert.setBigDecimal(2, BigDecimal.valueOf(stack));
                insert.setInt(3, version);
                insert.executeUpdate();
            }
            try (PreparedStatement insert = admin.prepareStatement(
                    "INSERT INTO app.default_range_hand (situation, stack, hand, action) VALUES (?, ?, ?, ?)")) {
                for (Map.Entry<String, String> hand : hands.entrySet()) {
                    insert.setString(1, situation);
                    insert.setBigDecimal(2, BigDecimal.valueOf(stack));
                    insert.setString(3, hand.getKey());
                    insert.setString(4, hand.getValue());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            admin.commit();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
