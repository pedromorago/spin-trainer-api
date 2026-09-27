package com.pedromorago.spintrainer.range;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The "3H OS call" table of the PDF is not a range but one threshold per hand ({@code reference-os-call-thresholds.json}):
 * the bb_vs_sb_os reference ranges in {@code reference-ranges.json} (seeded by V7) must be exactly its derivation, CALL
 * when the stack is at most the threshold. If either file changes without the other, this fails.
 */
class ReferenceThresholdsTest {

    static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void theBbVsSbOpenShoveRangesAreDerivedFromTheThresholds() throws IOException {
        JsonNode source = read("reference-os-call-thresholds.json");
        String situation = source.get("situation").asString();
        Map<String, BigDecimal> thresholds = new TreeMap<>();
        source.get("thresholds")
                .properties()
                .forEach(h -> thresholds.put(h.getKey(), h.getValue().decimalValue()));

        Map<BigDecimal, Map<String, String>> expected = new TreeMap<>();
        for (JsonNode stackNode : source.get("stacks")) {
            BigDecimal stack = stackNode.decimalValue();
            Map<String, String> calls = new TreeMap<>();
            thresholds.forEach((hand, threshold) -> {
                if (stack.compareTo(threshold) <= 0) {
                    calls.put(hand, "CALL");
                }
            });
            expected.put(stack, calls);
        }

        Map<BigDecimal, Map<String, String>> seeded = new TreeMap<>();
        for (JsonNode range : read("reference-ranges.json").get("ranges")) {
            if (range.get("situation").asString().equals(situation)) {
                Map<String, String> hands = new TreeMap<>();
                range.get("hands")
                        .properties()
                        .forEach(h -> hands.put(h.getKey(), h.getValue().asString()));
                seeded.put(range.get("stack").decimalValue(), hands);
            }
        }

        assertThat(thresholds).as("una celda por mano").hasSize(169);
        assertThat(seeded).isEqualTo(expected);
    }

    /** Boundary values of the rule, read from the table: a threshold equal to the stack calls, just below it folds. */
    @Test
    void aThresholdEqualToTheStackCalls() throws IOException {
        Map<BigDecimal, List<String>> calls = new TreeMap<>();
        for (JsonNode range : read("reference-ranges.json").get("ranges")) {
            if (range.get("situation").asString().equals("bb_vs_sb_os")) {
                List<String> hands = new ArrayList<>();
                range.get("hands").properties().forEach(h -> hands.add(h.getKey()));
                calls.put(range.get("stack").decimalValue(), hands);
            }
        }

        // Equal to the stack: QTs 20 (the cap), Q6s 10, Q7o 8 and T5o 4 call. Just below: QJo 19.9, Q5s 8.9, J8o 7.7,
        // 95o 3.7.
        assertThat(calls.get(BigDecimal.valueOf(20))).contains("QTs").doesNotContain("QJo");
        assertThat(calls.get(BigDecimal.valueOf(15))).contains("22", "QJo").doesNotContain("K6s");
        assertThat(calls.get(BigDecimal.valueOf(12))).contains("K4s").doesNotContain("K3s");
        assertThat(calls.get(BigDecimal.valueOf(10))).contains("Q6s").doesNotContain("Q5s");
        assertThat(calls.get(BigDecimal.valueOf(8))).contains("Q7o").doesNotContain("J8o");
        assertThat(calls.get(BigDecimal.valueOf(4))).contains("T5o").doesNotContain("95o");
    }

    private static JsonNode read(String file) throws IOException {
        return JSON.readTree(Files.readString(Path.of(file), StandardCharsets.UTF_8));
    }
}
