package com.pedromorago.spintrainer.range;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The example reference ranges (ADR-0024): {@code reference-ranges.json} is generated from
 * {@code reference-ranges-recipe.json}. The 169 hands are ranked by their all-in equity against one random hand
 * (Monte Carlo with a fixed seed, so the ranking is always the same); each range of the recipe lists bands of actions
 * with their share of the 1326 two-card combos, filled in ranking order, and the hands left take the implicit action.
 * Public and reproducible: examples to train with, not a strategy. {@code ./gradlew generateReferenceRanges} rewrites
 * the file; {@code ExampleRangesTest} fails while they differ.
 */
final class ExampleRanges {

    static final Path RECIPE = Path.of("reference-ranges-recipe.json");
    static final Path FILE = Path.of("reference-ranges.json");
    /** The grid's order: rows and columns from the ace down. */
    static final String RANKS = "AKQJT98765432";

    static final int COMBOS = 1326;
    static final int DEALS = 100_000;
    static final long SEED = 24;

    static final String DESCRIPTION = "Example reference ranges (ADR-0024), generated from reference-ranges-recipe.json"
            + " (./gradlew generateReferenceRanges): the hands, ranked by all-in equity against a random hand, fill"
            + " each range's bands of actions; the rest take the implicit action (FOLD, or CHECK if FOLD is not"
            + " possible). Examples to train with, not a strategy. Source of migration V9 and of the web and QA copies.";

    record Band(String action, int percent) {}

    record Recipe(String situation, BigDecimal stack, List<Band> bands) {}

    private static List<String> ranking;

    private ExampleRanges() {}

    public static void main(String[] args) throws IOException {
        Files.writeString(FILE, render(read(RECIPE)), StandardCharsets.UTF_8);
    }

    /** The 169 hands in grid order: row by row, the pair on the diagonal, suited above it and offsuit below. */
    static List<String> grid() {
        List<String> hands = new ArrayList<>();
        for (int row = 0; row < 13; row++) {
            for (int column = 0; column < 13; column++) {
                hands.add(hand(row, column));
            }
        }
        return hands;
    }

    static String hand(int row, int column) {
        if (row == column) {
            return RANKS.substring(row, row + 1).repeat(2);
        }
        return "" + RANKS.charAt(Math.min(row, column)) + RANKS.charAt(Math.max(row, column))
                + (row < column ? "s" : "o");
    }

    static int combos(String hand) {
        if (hand.length() == 2) {
            return 6;
        }
        return hand.endsWith("s") ? 4 : 12;
    }

    /** Best first, by equity; exact ties (none so far) go to the grid's order. */
    static synchronized List<String> ranking() {
        if (ranking == null) {
            List<String> grid = grid();
            double[] equities = new double[grid.size()];
            for (int i = 0; i < grid.size(); i++) {
                equities[i] = equity(grid.get(i), i);
            }
            ranking = IntStream.range(0, grid.size())
                    .boxed()
                    .sorted(Comparator.comparingDouble((Integer i) -> -equities[i]))
                    .map(grid::get)
                    .toList();
        }
        return ranking;
    }

    /**
     * Percent of the pot won all-in against one random hand: {@value #DEALS} deals of the opponent's two cards and the
     * five of the board, from a generator seeded per hand (its index in the grid).
     */
    static double equity(String hand, int index) {
        int[] own = cards(hand);
        int[] deck = new int[50];
        int size = 0;
        for (int card = 0; card < 52; card++) {
            if (card != own[0] && card != own[1]) {
                deck[size++] = card;
            }
        }
        SplittableRandom random = new SplittableRandom(SEED + index);
        int[] mine = new int[7];
        int[] theirs = new int[7];
        mine[0] = own[0];
        mine[1] = own[1];
        long halves = 0;
        for (int deal = 0; deal < DEALS; deal++) {
            for (int i = 0; i < 7; i++) {
                int j = i + random.nextInt(50 - i);
                int card = deck[i];
                deck[i] = deck[j];
                deck[j] = card;
            }
            theirs[0] = deck[0];
            theirs[1] = deck[1];
            for (int i = 2; i < 7; i++) {
                mine[i] = deck[i];
                theirs[i] = deck[i];
            }
            int difference = Integer.compare(evaluate(mine), evaluate(theirs));
            halves += difference + 1;
        }
        return halves * 50.0 / DEALS;
    }

    /** A concrete combo of the hand: card = rank (0 = deuce .. 12 = ace) × 4 + suit. */
    static int[] cards(String hand) {
        int high = 12 - RANKS.indexOf(hand.charAt(0));
        int low = 12 - RANKS.indexOf(hand.charAt(1));
        int lowSuit = hand.endsWith("s") ? 0 : 1;
        return new int[] {high * 4, low * 4 + lowSuit};
    }

    /** The best five of seven cards as a number: the category (high card 0 .. straight flush 8), then the ranks. */
    static int evaluate(int[] cards) {
        int[] counts = new int[13];
        int[] suits = new int[4];
        int ranks = 0;
        for (int card : cards) {
            counts[card >> 2]++;
            suits[card & 3] |= 1 << (card >> 2);
            ranks |= 1 << (card >> 2);
        }
        // Seven cards cannot hold a flush and also quads or a full house: the flush decides.
        for (int suit : suits) {
            if (Integer.bitCount(suit) >= 5) {
                int straight = straight(suit);
                return straight >= 0 ? score(8, straight) : score(5, top(suit, 5));
            }
        }
        int quads = -1;
        int trips = -1;
        int pair = -1;
        int second = -1;
        for (int rank = 12; rank >= 0; rank--) {
            if (counts[rank] == 4) {
                quads = rank;
            } else if (counts[rank] == 3 && trips < 0) {
                trips = rank;
            } else if (counts[rank] >= 2 && pair < 0) {
                pair = rank;
            } else if (counts[rank] == 2 && second < 0) {
                second = rank;
            }
        }
        if (quads >= 0) {
            return score(7, quads << 4 | top(ranks & ~(1 << quads), 1));
        }
        if (trips >= 0 && pair >= 0) {
            return score(6, trips << 4 | pair);
        }
        int straight = straight(ranks);
        if (straight >= 0) {
            return score(4, straight);
        }
        if (trips >= 0) {
            return score(3, trips << 8 | top(ranks & ~(1 << trips), 2));
        }
        if (pair >= 0 && second >= 0) {
            return score(2, pair << 8 | second << 4 | top(ranks & ~(1 << pair) & ~(1 << second), 1));
        }
        if (pair >= 0) {
            return score(1, pair << 12 | top(ranks & ~(1 << pair), 3));
        }
        return score(0, top(ranks, 5));
    }

    private static int score(int category, int ranks) {
        return category << 20 | ranks;
    }

    /** The highest card of the straight in the mask of ranks (3 = the wheel's five), or -1. */
    static int straight(int ranks) {
        int withLowAce = ranks << 1 | (ranks >> 12 & 1);
        for (int high = 13; high >= 4; high--) {
            if ((withLowAce >> (high - 4) & 0b11111) == 0b11111) {
                return high - 1;
            }
        }
        return -1;
    }

    /** The {@code count} highest ranks of the mask, four bits each, the highest first. */
    private static int top(int ranks, int count) {
        int packed = 0;
        for (int rank = 12; rank >= 0 && count > 0; rank--) {
            if ((ranks >> rank & 1) != 0) {
                packed = packed << 4 | rank;
                count--;
            }
        }
        return packed;
    }

    /**
     * The hands of the bands, in ranking order: a hand goes to the first band whose share of the combos is not yet
     * filled when its turn comes; once the last one is, the rest take the implicit action and are left out.
     */
    static Map<String, String> range(List<Band> bands) {
        Map<String, String> hands = new LinkedHashMap<>();
        if (bands.isEmpty()) {
            return hands;
        }
        int placed = 0;
        int band = 0;
        int filled = bands.getFirst().percent();
        for (String hand : ranking()) {
            while (placed * 100 >= filled * COMBOS && ++band < bands.size()) {
                filled += bands.get(band).percent();
            }
            if (band == bands.size()) {
                break;
            }
            hands.put(hand, bands.get(band).action());
            placed += combos(hand);
        }
        return hands;
    }

    static List<Recipe> read(Path recipe) throws IOException {
        JsonNode root = JsonMapper.builder().build().readTree(Files.readString(recipe, StandardCharsets.UTF_8));
        List<Recipe> recipes = new ArrayList<>();
        for (JsonNode range : root.get("ranges")) {
            List<Band> bands = new ArrayList<>();
            for (JsonNode band : range.get("bands")) {
                bands.add(new Band(band.get(0).asString(), band.get(1).asInt()));
            }
            recipes.add(new Recipe(
                    range.get("situation").asString(), range.get("stack").decimalValue(), bands));
        }
        return recipes;
    }

    /** The file: one line per row of the grid with the hands of explicit action, like the migration that loads it. */
    static String render(List<Recipe> recipes) {
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"description\": \"").append(DESCRIPTION).append("\",\n  \"ranges\": [\n");
        for (int i = 0; i < recipes.size(); i++) {
            Recipe recipe = recipes.get(i);
            Map<String, String> hands = range(recipe.bands());
            List<String> rows = new ArrayList<>();
            for (int row = 0; row < 13; row++) {
                int r = row;
                String line = IntStream.range(0, 13)
                        .mapToObj(column -> hand(r, column))
                        .filter(hands::containsKey)
                        .map(hand -> "\"" + hand + "\": \"" + hands.get(hand) + "\"")
                        .collect(Collectors.joining(", "));
                if (!line.isEmpty()) {
                    rows.add("        " + line);
                }
            }
            json.append("    {\n      \"situation\": \"")
                    .append(recipe.situation())
                    .append("\",\n      \"stack\": ")
                    .append(recipe.stack().stripTrailingZeros().toPlainString())
                    .append(",\n      \"hands\": {\n")
                    .append(String.join(",\n", rows))
                    .append(rows.isEmpty() ? "" : "\n")
                    .append("      }\n")
                    .append(i < recipes.size() - 1 ? "    },\n" : "    }\n");
        }
        return json.append("  ]\n}\n").toString();
    }
}
