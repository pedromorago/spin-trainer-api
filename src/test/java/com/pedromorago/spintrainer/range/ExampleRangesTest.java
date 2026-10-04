package com.pedromorago.spintrainer.range;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.pedromorago.spintrainer.range.ExampleRanges.Band;
import com.pedromorago.spintrainer.range.ExampleRanges.Recipe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * The example reference ranges (ADR-0024) are exactly what the recipe gives ({@code reference-ranges.json} is not
 * edited by hand), and the parts of the generator do what they say.
 */
class ExampleRangesTest {

    @Test
    void theFileIsWhatTheRecipeGives() throws IOException {
        String file = Files.readString(ExampleRanges.FILE, StandardCharsets.UTF_8);

        assertThat(file)
                .as("reference-ranges.json is out of date: ./gradlew generateReferenceRanges, and a new migration")
                .isEqualTo(ExampleRanges.render(ExampleRanges.read(ExampleRanges.RECIPE)));
    }

    @Test
    void theRecipeHasOneRangePerSpotAndNoImplicitAction() throws IOException {
        List<Recipe> recipes = ExampleRanges.read(ExampleRanges.RECIPE);

        assertThat(recipes).hasSize(80);
        assertThat(recipes.stream().map(r -> r.situation() + "@" + r.stack()).distinct())
                .hasSize(80);
        assertThat(recipes).flatMap(Recipe::bands).allSatisfy(band -> {
            assertThat(band.action()).isNotIn("FOLD", "CHECK");
            assertThat(band.percent()).isBetween(1, 100);
        });
        assertThat(recipes)
                .allSatisfy(recipe -> assertThat(
                                recipe.bands().stream().mapToInt(Band::percent).sum())
                        .isLessThanOrEqualTo(100));
    }

    @Test
    void theGridHasEveryHandOnceInRowOrder() {
        List<String> grid = ExampleRanges.grid();

        assertThat(grid).hasSize(169).doesNotHaveDuplicates();
        assertThat(grid.subList(0, 3)).containsExactly("AA", "AKs", "AQs");
        assertThat(grid.get(13)).isEqualTo("AKo");
        assertThat(grid.getLast()).isEqualTo("22");
        assertThat(grid.stream().mapToInt(ExampleRanges::combos).sum()).isEqualTo(ExampleRanges.COMBOS);
    }

    /** Known all-in equities against a random hand; the Monte Carlo lands within a few tenths of them. */
    @Test
    void equitiesMatchTheKnownOnes() {
        List<String> grid = ExampleRanges.grid();

        assertThat(ExampleRanges.equity("AA", grid.indexOf("AA"))).isCloseTo(85.2, within(0.6));
        assertThat(ExampleRanges.equity("KK", grid.indexOf("KK"))).isCloseTo(82.4, within(0.6));
        assertThat(ExampleRanges.equity("AKs", grid.indexOf("AKs"))).isCloseTo(67.0, within(0.6));
        assertThat(ExampleRanges.equity("AKo", grid.indexOf("AKo"))).isCloseTo(65.4, within(0.6));
        assertThat(ExampleRanges.equity("72o", grid.indexOf("72o"))).isCloseTo(34.6, within(0.6));
    }

    @Test
    void theRankingGoesFromAcesToTreyDeuce() {
        List<String> ranking = ExampleRanges.ranking();

        assertThat(ranking).hasSize(169).doesNotHaveDuplicates();
        assertThat(ranking.subList(0, 3)).containsExactly("AA", "KK", "QQ");
        assertThat(ranking.getLast()).isEqualTo("32o");
        assertThat(ranking.indexOf("AKs")).isLessThan(ranking.indexOf("AKo"));
    }

    @Test
    void handsAreRankedByTheirBestFiveCards() {
        // Cards: rank (0 = deuce .. 12 = ace) × 4 + suit.
        int royalFlush = ExampleRanges.evaluate(cards(12, 0, 11, 0, 10, 0, 9, 0, 8, 0, 0, 1, 1, 2));
        int quads = ExampleRanges.evaluate(cards(12, 0, 12, 1, 12, 2, 12, 3, 11, 0, 0, 1, 1, 2));
        int fullHouseFromTwoTrips = ExampleRanges.evaluate(cards(5, 0, 5, 1, 5, 2, 3, 0, 3, 1, 3, 2, 12, 3));
        int flush = ExampleRanges.evaluate(cards(12, 0, 9, 0, 6, 0, 3, 0, 0, 0, 11, 1, 11, 2));
        int sixHighStraight = ExampleRanges.evaluate(cards(4, 0, 3, 1, 2, 2, 1, 3, 0, 0, 11, 1, 9, 2));
        int wheel = ExampleRanges.evaluate(cards(12, 0, 3, 1, 2, 2, 1, 3, 0, 0, 11, 1, 9, 2));
        int trips = ExampleRanges.evaluate(cards(10, 0, 10, 1, 10, 2, 12, 3, 7, 0, 5, 1, 0, 2));
        int twoPairWithThirdPairKicker = ExampleRanges.evaluate(cards(10, 0, 10, 1, 8, 2, 8, 3, 6, 0, 6, 1, 0, 2));
        int twoPairWithAceKicker = ExampleRanges.evaluate(cards(10, 0, 10, 1, 8, 2, 8, 3, 12, 0, 6, 1, 0, 2));

        assertThat(royalFlush).isGreaterThan(quads);
        assertThat(quads).isGreaterThan(fullHouseFromTwoTrips);
        assertThat(fullHouseFromTwoTrips).isGreaterThan(flush);
        assertThat(flush).isGreaterThan(sixHighStraight);
        assertThat(sixHighStraight).isGreaterThan(wheel);
        assertThat(wheel).isGreaterThan(trips);
        assertThat(trips).isGreaterThan(twoPairWithAceKicker);
        assertThat(twoPairWithAceKicker).isGreaterThan(twoPairWithThirdPairKicker);
        assertThat(ExampleRanges.straight(0b1_0000_0000_1111)).as("the wheel").isEqualTo(3);
        assertThat(ExampleRanges.straight(0b1_1100_0000_0011))
                .as("Q-K-A-2-3 does not wrap around")
                .isEqualTo(-1);
    }

    @Test
    void bandsTakeTheirShareOfCombosInRankingOrder() {
        List<String> ranking = ExampleRanges.ranking();

        assertThat(ExampleRanges.range(List.of(new Band("ALLIN", 100)))).hasSize(169);
        assertThat(ExampleRanges.range(List.of())).isEmpty();
        // 1 % = 13.26 combos: the pairs AA, KK and QQ start below it (0, 6, 12); the next one does not.
        assertThat(ExampleRanges.range(List.of(new Band("ALLIN", 1))).keySet()).containsExactly("AA", "KK", "QQ");
        var split = ExampleRanges.range(List.of(new Band("MR_4B_C", 1), new Band("MR_F_F", 1)));
        assertThat(split.keySet()).containsExactlyElementsOf(ranking.subList(0, split.size()));
        assertThat(split).containsEntry("AA", "MR_4B_C").containsEntry(ranking.get(3), "MR_F_F");
        Set<String> actions = split.values().stream().collect(Collectors.toSet());
        assertThat(actions).containsExactlyInAnyOrder("MR_4B_C", "MR_F_F");
    }

    private static int[] cards(int... rankSuit) {
        int[] cards = new int[rankSuit.length / 2];
        for (int i = 0; i < cards.length; i++) {
            cards[i] = rankSuit[2 * i] * 4 + rankSuit[2 * i + 1];
        }
        return cards;
    }
}
