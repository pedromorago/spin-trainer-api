package com.pedromorago.spintrainer.support;

import com.pedromorago.spintrainer.testsupport.PostgresTestDatabase;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

/**
 * Data that the API cannot write (spin_app only reads the reference ranges): inserted as administrator, just as a seed
 * migration would do it. Idempotent: the tests share the database.
 */
public final class TestData {

    private TestData() {}

    /** An attempt with a chosen date (the API always uses the current time), to test the per-day statistics. */
    public static void attempt(
            UUID user, String situation, double stack, String hand, String given, String expected, Instant answeredAt) {
        try (Connection admin = PostgresTestDatabase.shared().adminConnection();
                PreparedStatement insert = admin.prepareStatement("""
                        INSERT INTO app.quiz_attempt (id, user_id, situation, stack, hand, given, expected, correct,
                                                      range_source, range_version, answered_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'default', 1, ?)""")) {
            insert.setObject(1, UUID.randomUUID());
            insert.setObject(2, user);
            insert.setString(3, situation);
            insert.setBigDecimal(4, BigDecimal.valueOf(stack));
            insert.setString(5, hand);
            insert.setString(6, given);
            insert.setString(7, expected);
            insert.setBoolean(8, given.equals(expected));
            insert.setObject(9, answeredAt.atOffset(ZoneOffset.UTC));
            insert.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Sets (or, with null, removes) the notes of a situation: the seed has none (ADR-0024). */
    public static void situationNotes(String situation, String notes) {
        try (Connection admin = PostgresTestDatabase.shared().adminConnection();
                PreparedStatement update = admin.prepareStatement("UPDATE app.situation SET notes = ? WHERE key = ?")) {
            update.setString(1, notes);
            update.setString(2, situation);
            update.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Removes the reference range of a spot (the seed includes all of them): to test what happens without it. */
    public static void withoutDefaultRange(String situation, double stack) {
        try (Connection admin = PostgresTestDatabase.shared().adminConnection();
                PreparedStatement delete =
                        admin.prepareStatement("DELETE FROM app.default_range WHERE situation = ? AND stack = ?")) {
            delete.setString(1, situation);
            delete.setBigDecimal(2, BigDecimal.valueOf(stack));
            delete.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

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
