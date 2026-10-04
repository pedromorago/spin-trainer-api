package com.pedromorago.spintrainer.situation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

import com.pedromorago.spintrainer.situation.application.port.out.SituationRepository;
import com.pedromorago.spintrainer.situation.domain.Situation;
import com.pedromorago.spintrainer.support.ApiIntegrationTest;
import com.pedromorago.spintrainer.support.TestData;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class SituationsIT extends ApiIntegrationTest {

    final String auth = bearer(UUID.randomUUID());

    @Autowired
    SituationRepository repository;

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
        // V7: bb_vs_sb_os, right after the other BB vs SB spots.
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

    /**
     * The seed has no notes (ADR-0024): one is written for this test only. The service keeps the catalog it loaded, so
     * the repository (the catalog's source) is asked directly; the controller's mapping is SituationControllerTest's.
     */
    @Test
    void notesAreReadWhenASituationHasThem() {
        TestData.situationNotes("sb_open", "Limp more against passive players.");
        try {
            assertThat(repository.findAll())
                    .filteredOn(situation -> situation.key().value().equals("sb_open"))
                    .singleElement()
                    .extracting(Situation::notes)
                    .isEqualTo(Optional.of("Limp more against passive players."));
        } finally {
            TestData.situationNotes("sb_open", null);
        }
    }

    @Test
    void aSituationWithoutNotesOmitsTheField() {
        assertThat(listSituations())
                .bodyJson()
                .extractingPath("$[?(@.key == 'sb_vs_btn_limp')]")
                .asArray()
                .singleElement()
                .asInstanceOf(MAP)
                .doesNotContainKey("notes");
    }

    /** The notes of the first seed were a third party's advice (ADR-0024): no situation has notes. */
    @Test
    void theSeedHasNoNotes() {
        assertThat(listSituations())
                .bodyJson()
                .extractingPath("$[*].notes")
                .asArray()
                .isEmpty();
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
