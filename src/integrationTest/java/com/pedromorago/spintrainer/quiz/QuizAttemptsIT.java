package com.pedromorago.spintrainer.quiz;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.pedromorago.spintrainer.support.ApiIntegrationTest;
import com.pedromorago.spintrainer.support.TestData;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** El servidor corrige cada respuesta (ADR-0013) y guarda eventos inmutables (ADR-0007). */
class QuizAttemptsIT extends ApiIntegrationTest {

    static final String PATH = "/quiz/attempts";

    final String auth = bearer(UUID.randomUUID());

    @BeforeEach
    void referenceRanges() {
        TestData.defaultRange("btn_open", 25, 3, Map.of("AA", "MR_4B_C", "A5s", "MR_F_F"));
        TestData.defaultRange("bb_vs_sb_limp", 10, 1, Map.of("AA", "ALLIN"));
        TestData.withoutDefaultRange("btn_open", 8);
    }

    MvcTestResult record(String body) {
        return mvc.post()
                .uri("/api/v1/quiz/attempts")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    MvcTestResult record(String situation, String stack, String hand, String given) {
        return record("{\"situation\":\"%s\",\"stack\":%s,\"hand\":\"%s\",\"given\":\"%s\"}"
                .formatted(situation, stack, hand, given));
    }

    MvcTestResult list(String query) {
        return mvc.get()
                .uri("/api/v1/quiz/attempts" + query)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .exchange();
    }

    @Test
    void theServerGradesTheAnswerAgainstTheReferenceRange() {
        MvcTestResult result = record("btn_open", "25", "A5s", "MR_C_F");

        assertThat(result).hasStatus(201);
        CONTRACT.assertResponse("POST", PATH, result);
        assertThat(result).bodyJson().isLenientlyEqualTo("""
                        {"situation":"btn_open","stack":25,"hand":"A5s","given":"MR_C_F","expected":"MR_F_F",
                         "correct":false,"rangeSource":"default","rangeVersion":3}""");
    }

    @Test
    void handsOutsideTheRangeExpectTheImplicitAction() {
        assertThat(record("btn_open", "25", "72o", "FOLD"))
                .bodyJson()
                .isLenientlyEqualTo("{\"expected\":\"FOLD\",\"correct\":true}");
        assertThat(record("bb_vs_sb_limp", "10", "72o", "CHECK"))
                .bodyJson()
                .isLenientlyEqualTo("{\"expected\":\"CHECK\",\"correct\":true}");
    }

    @Test
    void theClientCannotSendTheGradeItself() {
        MvcTestResult result = mvc.post()
                .uri("/api/v1/quiz/attempts")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        "{\"situation\":\"btn_open\",\"stack\":25,\"hand\":\"72o\",\"given\":\"ALLIN\",\"correct\":true}")
                .exchange();

        assertThat(result).hasStatus(400);
        CONTRACT.assertResponse("POST", PATH, result);
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("correct");
    }

    @Test
    void theUserRangeTakesPrecedenceAndTheAttemptRemembersIt() {
        mvc.put()
                .uri("/api/v1/ranges/user/btn_open/25")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hands\":{\"A5s\":\"MR_C_F\"},\"version\":0}")
                .exchange();

        assertThat(record("btn_open", "25", "A5s", "MR_C_F"))
                .bodyJson()
                .isLenientlyEqualTo(
                        "{\"expected\":\"MR_C_F\",\"correct\":true,\"rangeSource\":\"user\",\"rangeVersion\":1}");
    }

    @Test
    void pastAttemptsKeepTheirGradeWhenTheRangeChanges() {
        record("btn_open", "25", "A5s", "MR_F_F");
        mvc.put()
                .uri("/api/v1/ranges/user/btn_open/25")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"hands\":{\"A5s\":\"ALLIN\"},\"version\":0}")
                .exchange();

        assertThat(list(""))
                .bodyJson()
                .extractingPath("$.items[0]")
                .asMap()
                .containsEntry("expected", "MR_F_F")
                .containsEntry("correct", true)
                .containsEntry("rangeSource", "default");
    }

    @Test
    void aSpotWithoutRangeCannotBeGraded() {
        MvcTestResult result = record("btn_open", "8", "AA", "MR_4B_C");

        assertThat(result).hasStatus(422);
        CONTRACT.assertResponse("POST", PATH, result);
        assertThat(result)
                .bodyJson()
                .isLenientlyEqualTo("{\"type\":\"urn:spin-trainer:no-range\",\"title\":\"No range\",\"status\":422}");
    }

    @Test
    void rejectsUnknownSpotsHandsAndActions() {
        MvcTestResult unknownSpot = record("btn_open", "12.5", "AA", "FOLD");
        MvcTestResult notCanonical = record("btn_open", "25", "KAs", "FOLD");
        MvcTestResult notAHand = record("btn_open", "25", "AAs", "FOLD");
        MvcTestResult notAllowed = record("btn_open", "25", "AA", "CHECK");

        assertThat(unknownSpot).hasStatus(404);
        CONTRACT.assertResponse("POST", PATH, unknownSpot);
        assertThat(notCanonical)
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("hand");
        assertThat(notAHand)
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("hand");
        assertThat(notAllowed)
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.errors[0].field")
                .isEqualTo("given");
    }

    @Test
    void rejectsValuesJacksonWouldOtherwiseCoerce() {
        for (String body : List.of(
                "{\"situation\":\"btn_open\",\"stack\":\"25\",\"hand\":\"AA\",\"given\":\"FOLD\"}",
                "{\"situation\":\"btn_open\",\"stack\":25,\"hand\":\"AA\",\"given\":17}",
                "{\"situation\":\"btn_open\",\"stack\":25,\"hand\":\"AA\",\"given\":null}",
                "{\"situation\":\"btn_open\",\"stack\":true,\"hand\":\"AA\",\"given\":\"FOLD\"}")) {
            MvcTestResult result = record(body);
            assertThat(result).as(body).hasStatus(400);
            CONTRACT.assertResponse("POST", PATH, result);
        }
    }

    @Test
    void pagesFromNewestToOldestWithoutGapsOrDuplicates() {
        for (int i = 0; i < 5; i++) {
            assertThat(record("btn_open", "25", "AA", i % 2 == 0 ? "MR_4B_C" : "FOLD"))
                    .hasStatus(201);
        }
        List<Map<String, Object>> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            MvcTestResult page = list("?limit=2" + (cursor == null ? "" : "&cursor=" + cursor));
            assertThat(page).hasStatusOk();
            CONTRACT.assertResponse("GET", PATH, page);
            List<Map<String, Object>> items = readItems(page);
            assertThat(items).hasSizeLessThanOrEqualTo(2);
            seen.addAll(items);
            cursor = readCursor(page);
            pages++;
        } while (cursor != null);

        assertThat(pages).isEqualTo(3);
        assertThat(seen).hasSize(5);
        assertThat(new HashSet<>(seen.stream().map(item -> item.get("id")).toList()))
                .hasSize(5);
        assertThat(seen.stream()
                        .map(item -> Instant.parse((String) item.get("answeredAt")))
                        .toList())
                .isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @Test
    void filtersBySituationAndStack() {
        record("btn_open", "25", "AA", "MR_4B_C");
        record("bb_vs_sb_limp", "10", "AA", "ALLIN");

        assertThat(list("?situation=bb_vs_sb_limp"))
                .bodyJson()
                .extractingPath("$.items[*].situation")
                .asArray()
                .containsExactly("bb_vs_sb_limp");
        assertThat(list("?situation=btn_open&stack=25.0"))
                .bodyJson()
                .extractingPath("$.items.length()")
                .isEqualTo(1);
        assertThat(list("?stack=12.5"))
                .bodyJson()
                .extractingPath("$.items")
                .asArray()
                .isEmpty();
    }

    @Test
    void theLastPageSaysSoWithANullCursor() {
        MvcTestResult empty = list("");

        CONTRACT.assertResponse("GET", PATH, empty);
        assertThat(empty).bodyJson().isStrictlyEqualTo("{\"items\":[],\"nextCursor\":null}");
    }

    @Test
    void rejectsInvalidPagingParameters() {
        for (String query :
                List.of("?limit=0", "?limit=201", "?limit=x", "?cursor=bm9wZQ", "?stack=12.3", "?situation=BTN")) {
            MvcTestResult result = list(query);
            assertThat(result).as(query).hasStatus(400);
            CONTRACT.assertResponse("GET", PATH, result);
        }
    }

    @Test
    void eachUserOnlySeesTheirOwnAttempts() {
        record("btn_open", "25", "AA", "MR_4B_C");

        assertThat(mvc.get()
                        .uri("/api/v1/quiz/attempts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID()))
                        .exchange())
                .bodyJson()
                .extractingPath("$.items")
                .asArray()
                .isEmpty();
    }

    private static List<Map<String, Object>> readItems(MvcTestResult page) {
        return JsonPath.read(contentOf(page), "$.items");
    }

    private static String readCursor(MvcTestResult page) {
        return JsonPath.read(contentOf(page), "$.nextCursor");
    }

    private static String contentOf(MvcTestResult page) {
        try {
            return page.getResponse().getContentAsString();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
