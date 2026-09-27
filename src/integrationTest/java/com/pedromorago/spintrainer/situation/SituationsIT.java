package com.pedromorago.spintrainer.situation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

import com.pedromorago.spintrainer.support.ApiIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class SituationsIT extends ApiIntegrationTest {

    final String auth = bearer(UUID.randomUUID());

    private MvcTestResult listSituations() {
        return mvc.get()
                .uri("/api/v1/situations")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .exchange();
    }

    @Test
    void servesTheSeventeenSituationsInPresentationOrderAsTheContractSays() {
        MvcTestResult result = listSituations();

        assertThat(result).hasStatusOk();
        CONTRACT.assertResponse("GET", "/situations", result);
        assertThat(result).bodyJson().extractingPath("$.length()").isEqualTo(17);
        assertThat(result).bodyJson().extractingPath("$[0].key").isEqualTo("btn_open");
        // V7: the "3H OS call" table, right after the other BB vs SB spots.
        assertThat(result).bodyJson().extractingPath("$[6].key").isEqualTo("bb_vs_sb_os");
        assertThat(result).bodyJson().extractingPath("$[7].key").isEqualTo("bb_vs_btn_mr_sb_fold");
        assertThat(result).bodyJson().extractingPath("$[16].key").isEqualTo("hu_bb_vs_os");
    }

    @Test
    void mapsEverySituationField() {
        assertThat(listSituations())
                .bodyJson()
                .extractingPath("$[?(@.key == 'bb_vs_sb_mr')]")
                .asArray()
                .singleElement()
                .isEqualTo(java.util.Map.of(
                        "key", "bb_vs_sb_mr",
                        "label", "BB vs SB Min-Raise",
                        "format", "3max",
                        "hero", "BB",
                        "priorActions",
                                java.util.List.of(
                                        java.util.Map.of("position", "BTN", "action", "FOLD"),
                                        java.util.Map.of("position", "SB", "action", "MIN_RAISE")),
                        "stacks", java.util.List.of(25, 20, 15, 10),
                        "actions", java.util.List.of("ALLIN", "3BET_C", "CALL", "CALL_VS_X2", "FOLD")));
    }

    @Test
    void stacksAreCanonicalNumbersIncludingHalves() {
        assertThat(listSituations())
                .bodyJson()
                .extractingPath("$[?(@.key == 'bb_vs_btn_mr_sb_3bet')].stacks[*]")
                .asArray()
                .containsExactly(25, 12.5, 10);
    }

    @Test
    void notesAreOnlyPresentWhenTheSituationHasThem() {
        MvcTestResult result = listSituations();

        assertThat(result)
                .bodyJson()
                .extractingPath("$[?(@.key == 'sb_open')].notes")
                .asArray()
                .singleElement()
                .asString()
                .startsWith("The suited part of yellow");
        assertThat(result)
                .bodyJson()
                .extractingPath("$[?(@.key == 'sb_vs_btn_limp')]")
                .asArray()
                .singleElement()
                .asInstanceOf(MAP)
                .doesNotContainKey("notes");
    }

    /** The app is in English (ADR-0021, migration V8): no note keeps the Spanish of the first seed. */
    @Test
    void everyNoteIsInEnglish() {
        assertThat(listSituations())
                .bodyJson()
                .extractingPath("$[*].notes")
                .asArray()
                .hasSize(4)
                .allSatisfy(note -> assertThat((String) note).matches("[\\x20-\\x7E]+"));
    }

    @Test
    void theCatalogIsRevalidatedWithAnETag() {
        MvcTestResult first = listSituations();
        String etag = first.getResponse().getHeader(HttpHeaders.ETAG);

        assertThat(etag).isNotBlank();
        assertThat(first.getResponse().getHeader(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-cache, private");

        MvcTestResult second = mvc.get()
                .uri("/api/v1/situations")
                .header(HttpHeaders.AUTHORIZATION, auth)
                .header(HttpHeaders.IF_NONE_MATCH, etag)
                .exchange();

        assertThat(second).hasStatus(304);
        CONTRACT.assertResponse("GET", "/situations", second);
    }

    @Test
    void withoutTokenTheErrorAlsoFollowsTheContract() {
        MvcTestResult result = mvc.get().uri("/api/v1/situations").exchange();

        assertThat(result).hasStatus(401);
        CONTRACT.assertResponse("GET", "/situations", result);
    }
}
