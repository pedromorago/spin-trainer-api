package com.pedromorago.spintrainer.range;

import static org.assertj.core.api.Assertions.assertThat;

import com.pedromorago.spintrainer.support.ApiIntegrationTest;
import com.pedromorago.spintrainer.support.TestData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class RangesIT extends ApiIntegrationTest {

    static final String DEFAULT_PATH = "/ranges/default/{situation}/{stack}";
    static final String USER_PATH = "/ranges/user/{situation}/{stack}";

    final String auth = bearer(UUID.randomUUID());

    // Después de arrancar el contexto (Flyway ya migró): en un @BeforeAll la tabla aún no existiría.
    @BeforeEach
    void referenceRanges() {
        TestData.defaultRange("btn_open", 25, 1, Map.of("AA", "MR_4B_C", "KK", "MR_4B_C", "22", "L_C_C"));
        TestData.defaultRange("bb_vs_btn_mr_sb_3bet", 12.5, 2, Map.of("AA", "ALLIN"));
    }

    MvcTestResult get(String uri) {
        return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, auth).exchange();
    }

    MvcTestResult put(String uri, String body) {
        return mvc.put()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    MvcTestResult delete(String uri) {
        return mvc.delete().uri(uri).header(HttpHeaders.AUTHORIZATION, auth).exchange();
    }

    @Nested
    class ReferenceRanges {

        @Test
        void listsEveryLoadedRangeInCatalogOrder() {
            MvcTestResult result = get("/api/v1/ranges/default");

            assertThat(result).hasStatusOk();
            CONTRACT.assertResponse("GET", "/ranges/default", result);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$[*].situation")
                    .asArray()
                    .containsSubsequence("btn_open", "bb_vs_btn_mr_sb_3bet");
        }

        @Test
        void servesOneRangeWithItsSeedVersion() {
            MvcTestResult result = get("/api/v1/ranges/default/bb_vs_btn_mr_sb_3bet/12.5");

            assertThat(result).hasStatusOk();
            CONTRACT.assertResponse("GET", DEFAULT_PATH, result);
            assertThat(result).bodyJson().isStrictlyEqualTo("""
                            {"situation":"bb_vs_btn_mr_sb_3bet","stack":12.5,"hands":{"AA":"ALLIN"},
                             "source":"default","version":2}""");
        }

        @Test
        void acceptsNonCanonicalStackNotation() {
            assertThat(get("/api/v1/ranges/default/btn_open/25.0"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.stack")
                    .isEqualTo(25);
        }

        @Test
        void aKnownSpotWithoutSeedIsNotFound() {
            MvcTestResult result = get("/api/v1/ranges/default/btn_open/8");

            assertThat(result).hasStatus(404);
            CONTRACT.assertResponse("GET", DEFAULT_PATH, result);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.detail")
                    .isEqualTo("Sin rango de referencia para btn_open@8");
        }

        @Test
        void anUnknownSpotIsNotFound() {
            assertThat(get("/api/v1/ranges/default/mtt_open/25")).hasStatus(404);
            assertThat(get("/api/v1/ranges/default/btn_open/12.5")).hasStatus(404);
        }

        @Test
        void malformedPathParametersAreValidationErrors() {
            MvcTestResult badStack = get("/api/v1/ranges/default/btn_open/12.3");
            MvcTestResult badSituation = get("/api/v1/ranges/default/BTN/25");

            assertThat(badStack).hasStatus(400);
            CONTRACT.assertResponse("GET", DEFAULT_PATH, badStack);
            assertThat(badStack).bodyJson().extractingPath("$.errors[0].field").isEqualTo("stack");
            assertThat(badSituation).hasStatus(400);
            assertThat(badSituation)
                    .bodyJson()
                    .extractingPath("$.errors[0].field")
                    .isEqualTo("situation");
        }

        @Test
        void isRevalidatedWithAnETag() {
            MvcTestResult first = get("/api/v1/ranges/default/btn_open/25");
            MvcTestResult second = mvc.get()
                    .uri("/api/v1/ranges/default/btn_open/25")
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .header(HttpHeaders.IF_NONE_MATCH, first.getResponse().getHeader(HttpHeaders.ETAG))
                    .exchange();

            assertThat(second).hasStatus(304);
            CONTRACT.assertResponse("GET", DEFAULT_PATH, second);
        }
    }

    @Nested
    class UserRanges {

        static final String URI = "/api/v1/ranges/user/btn_open/25";

        @Test
        void createReadReplaceDeleteLifecycle() throws Exception {
            MvcTestResult created =
                    put(URI, "{\"hands\":{\"AA\":\"MR_4B_C\",\"72o\":\"FOLD\",\"KK\":\"MR_C_C\"},\"version\":0}");
            assertThat(created).hasStatus(201);
            CONTRACT.assertResponse("PUT", USER_PATH, created);
            assertThat(created).bodyJson().isLenientlyEqualTo("""
                            {"situation":"btn_open","stack":25,"hands":{"AA":"MR_4B_C","KK":"MR_C_C"},
                             "source":"user","version":1}""");
            assertThat(created).bodyJson().extractingPath("$.updatedAt").isNotNull();

            MvcTestResult read = get(URI);
            assertThat(read).hasStatusOk();
            CONTRACT.assertResponse("GET", USER_PATH, read);
            assertThat(read).bodyJson().extractingPath("$.version").isEqualTo(1);
            assertThat(read)
                    .as("lo que devolvió el PUT es lo que se guardó")
                    .bodyJson()
                    .isStrictlyEqualTo(created.getResponse().getContentAsString());

            MvcTestResult replaced = put(URI, "{\"hands\":{\"QQ\":\"MR_4B_C\"},\"version\":1}");
            assertThat(replaced).hasStatus(200);
            CONTRACT.assertResponse("PUT", USER_PATH, replaced);
            assertThat(replaced).bodyJson().isLenientlyEqualTo("{\"hands\":{\"QQ\":\"MR_4B_C\"},\"version\":2}");

            MvcTestResult deleted = delete(URI);
            assertThat(deleted).hasStatus(204);
            CONTRACT.assertResponse("DELETE", USER_PATH, deleted);
            assertThat(get(URI)).hasStatus(404);
            assertThat(delete(URI)).as("idempotente").hasStatus(204);
        }

        @Test
        void listsOnlyTheCallersRanges() {
            put(URI, "{\"hands\":{},\"version\":0}");
            put("/api/v1/ranges/user/bb_vs_btn_mr_sb_3bet/12.5", "{\"hands\":{\"AA\":\"ALLIN\"},\"version\":0}");

            MvcTestResult mine = get("/api/v1/ranges/user");
            MvcTestResult someoneElses = mvc.get()
                    .uri("/api/v1/ranges/user")
                    .header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID()))
                    .exchange();

            CONTRACT.assertResponse("GET", "/ranges/user", mine);
            assertThat(mine)
                    .bodyJson()
                    .extractingPath("$[*].situation")
                    .asArray()
                    .containsExactly("btn_open", "bb_vs_btn_mr_sb_3bet");
            assertThat(someoneElses).bodyJson().isStrictlyEqualTo("[]");
        }

        @Test
        void aStaleVersionIsAConflict() {
            put(URI, "{\"hands\":{},\"version\":0}");
            put(URI, "{\"hands\":{},\"version\":1}");

            MvcTestResult stale = put(URI, "{\"hands\":{\"AA\":\"ALLIN\"},\"version\":1}");
            MvcTestResult recreate = put(URI, "{\"hands\":{},\"version\":0}");

            assertThat(stale).hasStatus(409);
            CONTRACT.assertResponse("PUT", USER_PATH, stale);
            assertThat(stale).bodyJson().extractingPath("$.detail").isEqualTo("El rango está en la versión 2; recarga");
            assertThat(recreate).hasStatus(409);
            assertThat(get(URI)).bodyJson().extractingPath("$.hands").isEqualTo(Map.of());
        }

        @Test
        void replacingADeletedRangeIsAConflict() {
            put(URI, "{\"hands\":{},\"version\":0}");
            delete(URI);

            assertThat(put(URI, "{\"hands\":{},\"version\":1}"))
                    .hasStatus(409)
                    .bodyJson()
                    .extractingPath("$.detail")
                    .isEqualTo("El rango personalizado ya no existe; recarga");
        }

        @Test
        void concurrentWritesOnTheSameVersionHaveExactlyOneWinner() throws Exception {
            put(URI, "{\"hands\":{},\"version\":0}");
            int writers = 8;
            CountDownLatch start = new CountDownLatch(1);
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < writers; i++) {
                String hands = "{\"hands\":{\"AA\":\"" + (i % 2 == 0 ? "ALLIN" : "MR_C_C") + "\"},\"version\":1}";
                tasks.add(() -> {
                    start.await();
                    return put(URI, hands).getResponse().getStatus();
                });
            }
            List<Integer> statuses = new ArrayList<>();
            try (ExecutorService pool = Executors.newFixedThreadPool(writers)) {
                List<Future<Integer>> futures = tasks.stream().map(pool::submit).toList();
                start.countDown();
                for (Future<Integer> future : futures) {
                    statuses.add(future.get());
                }
            }

            assertThat(statuses)
                    .containsOnly(200, 409)
                    .filteredOn(s -> s == 200)
                    .hasSize(1);
            assertThat(get(URI)).bodyJson().extractingPath("$.version").isEqualTo(2);
        }

        @Test
        void rejectsInvalidHandsAndActionsWithOneErrorEach() {
            MvcTestResult result = put(URI, """
                    {"hands":{"AAs":"MR_4B_C","KAs":"MR_4B_C","QQ":"CHECK","JJ":"MR_C_C"},"version":0}""");

            assertThat(result).hasStatus(400);
            CONTRACT.assertResponse("PUT", USER_PATH, result);
            assertThat(result).bodyJson().isLenientlyEqualTo("""
                            {"type":"urn:spin-trainer:validation","detail":"3 entradas no válidas en hands","errors":[
                              {"field":"hands.AAs","message":"mano no válida"},
                              {"field":"hands.KAs","message":"mano no válida"},
                              {"field":"hands.QQ","message":"acción CHECK no permitida en btn_open"}]}""");
            assertThat(get(URI)).as("no se guardó nada").hasStatus(404);
        }

        @Test
        void rejectsBodiesOutsideTheContract() {
            assertThat(put(URI, "{\"hands\":{\"AA\":\"RAISE\"},\"version\":0}"))
                    .hasStatus(400)
                    .bodyJson()
                    .extractingPath("$.errors[0].field")
                    .isEqualTo("hands.AA");
            assertThat(put(URI, "{\"hands\":{},\"version\":0,\"source\":\"default\"}"))
                    .hasStatus(400)
                    .bodyJson()
                    .extractingPath("$.errors[0].field")
                    .isEqualTo("source");
            assertThat(put(URI, "{\"hands\":{}}"))
                    .hasStatus(400)
                    .bodyJson()
                    .extractingPath("$.errors[0].field")
                    .isEqualTo("version");
            assertThat(put(URI, "{\"hands\":{},\"version\":-1}"))
                    .hasStatus(400)
                    .bodyJson()
                    .extractingPath("$.errors[0].field")
                    .isEqualTo("version");
        }

        @Test
        void rejectsValuesJacksonWouldOtherwiseCoerce() {
            for (String body : List.of(
                    "{\"hands\":{\"AA\":null},\"version\":0}",
                    "{\"hands\":{},\"version\":0.9}",
                    "{\"hands\":{},\"version\":\"0\"}",
                    "{\"hands\":{\"AA\":9},\"version\":0}",
                    "{\"hands\":[],\"version\":0}")) {
                MvcTestResult result = put(URI, body);
                assertThat(result).as(body).hasStatus(400);
                CONTRACT.assertResponse("PUT", USER_PATH, result);
            }
            assertThat(get(URI)).as("no se guardó nada").hasStatus(404);
        }

        @Test
        void aNullActionIsReportedForItsHand() {
            assertThat(put(URI, "{\"hands\":{\"AA\":null},\"version\":0}"))
                    .bodyJson()
                    .isLenientlyEqualTo("{\"errors\":[{\"field\":\"hands.AA\"}]}");
        }

        @Test
        void theHighestVersionCannotBeReplaced() {
            MvcTestResult result = put(URI, "{\"hands\":{},\"version\":2147483647}");

            assertThat(result).hasStatus(409);
            CONTRACT.assertResponse("PUT", USER_PATH, result);
        }

        @Test
        void oversizedDocumentsAreRejectedWithoutReadingThemWhole() {
            // JSON válido (espacios entre tokens) que sin el límite de tamaño daría 201.
            String huge = "{\"hands\":{}," + " ".repeat(70_000) + "\"version\":0}";

            MvcTestResult result = put(URI, huge);

            assertThat(result).hasStatus(400);
            CONTRACT.assertResponse("PUT", USER_PATH, result);
            assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("body");
            assertThat(put(URI, "{\"hands\":{}," + " ".repeat(1_000) + "\"version\":0}"))
                    .as("un documento normal con espacios sí vale")
                    .hasStatus(201);
        }

        @Test
        void anUnknownSpotIsNotFoundForEveryOperation() {
            String unknown = "/api/v1/ranges/user/btn_open/12.5";

            assertThat(get(unknown)).hasStatus(404);
            assertThat(put(unknown, "{\"hands\":{},\"version\":0}")).hasStatus(404);
            MvcTestResult deleted = delete(unknown);
            assertThat(deleted).hasStatus(404);
            CONTRACT.assertResponse("DELETE", USER_PATH, deleted);
            CONTRACT.assertResponse("DELETE", USER_PATH, delete("/api/v1/ranges/user/btn_open/12.3"));
        }
    }

    /**
     * La web manda {@code Accept: application/json}; los errores son {@code application/problem+json} (RFC 9457). El
     * DELETE, cuya única representación es un Problem, respondía 406 a esos clientes (hallado por los E2E de QA).
     */
    @Nested
    class JsonOnlyClients {

        static final String URI = "/api/v1/ranges/user/btn_open/25";

        MvcTestResult send(String method, String uri, MediaType accept) {
            return mvc.method(HttpMethod.valueOf(method))
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .accept(accept)
                    .exchange();
        }

        @Test
        void getDataAsJsonAndErrorsAsProblemDetails() {
            MvcTestResult reference = send("GET", "/api/v1/ranges/default/btn_open/25", MediaType.APPLICATION_JSON);
            MvcTestResult missing = send("GET", URI, MediaType.APPLICATION_JSON);

            assertThat(reference).hasStatus(200).hasContentType(MediaType.APPLICATION_JSON);
            assertThat(missing).hasStatus(404).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            CONTRACT.assertResponse("GET", USER_PATH, missing);
        }

        @Test
        void canDeleteARange() {
            put(URI, "{\"hands\":{},\"version\":0}");

            MvcTestResult deleted = send("DELETE", URI, MediaType.APPLICATION_JSON);
            MvcTestResult unknown = send("DELETE", "/api/v1/ranges/user/btn_open/12.5", MediaType.APPLICATION_JSON);

            assertThat(deleted).hasStatus(204);
            assertThat(get(URI)).hasStatus(404);
            assertThat(unknown).hasStatus(404).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
            CONTRACT.assertResponse("DELETE", USER_PATH, unknown);
        }

        @Test
        void aClientThatAcceptsNoJsonStillGetsA406() {
            assertThat(send("GET", "/api/v1/ranges/default/btn_open/25", MediaType.TEXT_HTML))
                    .hasStatus(406);
            assertThat(send("DELETE", URI, MediaType.TEXT_HTML)).hasStatus(406);
        }
    }
}
