package com.pedromorago.spintrainer.stats;

import static org.assertj.core.api.Assertions.assertThat;

import com.pedromorago.spintrainer.support.ApiIntegrationTest;
import com.pedromorago.spintrainer.support.TestData;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Aggregates over the attempts (ADR-0013), with the time fixed to test the day boundaries by time zone. */
@Import(StatsIT.FixedClock.class)
class StatsIT extends ApiIntegrationTest {

    static final Instant NOW = Instant.parse("2026-09-26T10:00:00Z");

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    final UUID user = UUID.randomUUID();

    @BeforeEach
    void attempts() {
        // 25/09 22:30 UTC is already 26/09 00:30 in Madrid (UTC+2).
        TestData.attempt(user, "btn_open", 25, "AA", "MR_4B_C", "MR_4B_C", Instant.parse("2026-09-25T22:30:00Z"));
        TestData.attempt(user, "btn_open", 25, "AA", "FOLD", "MR_4B_C", Instant.parse("2026-09-26T09:00:00Z"));
        TestData.attempt(user, "btn_open", 20, "K9s", "FOLD", "MR_F_F", Instant.parse("2026-09-20T12:00:00Z"));
        TestData.attempt(user, "hu_bb_vs_os", 12, "A2o", "CALL", "CALL", Instant.parse("2026-08-01T12:00:00Z"));
        TestData.attempt(UUID.randomUUID(), "btn_open", 25, "AA", "FOLD", "MR_4B_C", NOW.minusSeconds(60));
    }

    MvcTestResult get(String uri) {
        return mvc.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .exchange();
    }

    @Test
    void aggregatesAttemptsPerSituationStackAndHand() {
        MvcTestResult result = get("/api/v1/stats/hands");

        assertThat(result).hasStatusOk();
        CONTRACT.assertResponse("GET", "/stats/hands", result);
        assertThat(result).bodyJson().isStrictlyEqualTo("""
                        [{"situation":"btn_open","stack":25,"hand":"AA","attempts":2,"correct":1,
                          "lastAnsweredAt":"2026-09-26T09:00:00Z"},
                         {"situation":"btn_open","stack":20,"hand":"K9s","attempts":1,"correct":0,
                          "lastAnsweredAt":"2026-09-20T12:00:00Z"},
                         {"situation":"hu_bb_vs_os","stack":12,"hand":"A2o","attempts":1,"correct":1,
                          "lastAnsweredAt":"2026-08-01T12:00:00Z"}]""");
    }

    @Test
    void handStatsCanBeFiltered() {
        assertThat(get("/api/v1/stats/hands?situation=btn_open&stack=20"))
                .bodyJson()
                .extractingPath("$[*].hand")
                .asArray()
                .containsExactly("K9s");
        assertThat(get("/api/v1/stats/hands?situation=hu_bb_vs_os"))
                .bodyJson()
                .extractingPath("$[*].hand")
                .asArray()
                .containsExactly("A2o");
    }

    @ParameterizedTest(name = "days={0} tz={1}")
    @CsvSource(delimiter = '|', textBlock = """
            30 | UTC                 | [{"date":"2026-09-20","attempts":1,"correct":0},{"date":"2026-09-25","attempts":1,"correct":1},{"date":"2026-09-26","attempts":1,"correct":0}]
            30 | Europe/Madrid       | [{"date":"2026-09-20","attempts":1,"correct":0},{"date":"2026-09-26","attempts":2,"correct":1}]
            1  | UTC                 | [{"date":"2026-09-26","attempts":1,"correct":0}]
            1  | Europe/Madrid       | [{"date":"2026-09-26","attempts":2,"correct":1}]
            1  | CET                 | [{"date":"2026-09-26","attempts":2,"correct":1}]
            7  | America/Los_Angeles | [{"date":"2026-09-20","attempts":1,"correct":0},{"date":"2026-09-25","attempts":1,"correct":1},{"date":"2026-09-26","attempts":1,"correct":0}]
            90 | UTC                 | [{"date":"2026-08-01","attempts":1,"correct":1},{"date":"2026-09-20","attempts":1,"correct":0},{"date":"2026-09-25","attempts":1,"correct":1},{"date":"2026-09-26","attempts":1,"correct":0}]
            """)
    void progressCutsDaysInTheRequestedTimeZone(int days, String tz, String expected) {
        MvcTestResult result = get("/api/v1/stats/progress?days=" + days + "&tz=" + tz);

        assertThat(result).hasStatusOk();
        CONTRACT.assertResponse("GET", "/stats/progress", result);
        assertThat(result).bodyJson().isStrictlyEqualTo(expected);
    }

    @Test
    void progressDefaultsToThirtyDaysInUtc() {
        assertThat(get("/api/v1/stats/progress"))
                .bodyJson()
                .extractingPath("$[*].date")
                .asArray()
                .containsExactly("2026-09-20", "2026-09-25", "2026-09-26");
    }

    @Test
    void rejectsInvalidParameters() {
        for (String query : List.of("days=0", "days=366", "days=x", "tz=Nope/Zone", "tz=%2B01:00")) {
            MvcTestResult result = get("/api/v1/stats/progress?" + query);
            assertThat(result).as(query).hasStatus(400);
            CONTRACT.assertResponse("GET", "/stats/progress", result);
        }
        MvcTestResult badStack = get("/api/v1/stats/hands?stack=12.3");
        assertThat(badStack).hasStatus(400);
        CONTRACT.assertResponse("GET", "/stats/hands", badStack);
    }

    @Test
    void aNewUserHasNoStats() {
        MvcTestResult hands = mvc.get()
                .uri("/api/v1/stats/hands")
                .header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID()))
                .exchange();

        assertThat(hands).bodyJson().isStrictlyEqualTo("[]");
    }
}
