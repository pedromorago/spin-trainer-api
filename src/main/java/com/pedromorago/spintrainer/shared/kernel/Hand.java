package com.pedromorago.spintrainer.shared.kernel;

import java.util.ArrayList;
import java.util.List;

/**
 * Mano canónica del grid 13×13: pareja ({@code AA}) o carta alta + carta baja + {@code s}/{@code o} ({@code AKs},
 * {@code T9o}). No son canónicas {@code AAs}, {@code AK} ni {@code KAs}. Mismas reglas que {@code domain/hand.js} de la
 * web.
 */
public record Hand(String code) implements Comparable<Hand> {

    private static final String RANKS = "AKQJT98765432";
    private static final List<Hand> ALL = grid();

    public Hand {
        if (!isCanonical(code)) {
            throw DomainException.validation("hand", "mano no válida");
        }
    }

    public static Hand of(String code) {
        return new Hand(code);
    }

    public static boolean isCanonical(String code) {
        if (code == null || code.length() < 2 || code.length() > 3) {
            return false;
        }
        int high = RANKS.indexOf(code.charAt(0));
        int low = RANKS.indexOf(code.charAt(1));
        if (high < 0 || low < 0) {
            return false;
        }
        if (code.length() == 2) {
            return high == low;
        }
        char kind = code.charAt(2);
        return high < low && (kind == 's' || kind == 'o');
    }

    /** Las 169 manos, fila a fila del grid (AA, AKs, AQs... 32o, 22). */
    public static List<Hand> all() {
        return ALL;
    }

    public boolean isPair() {
        return code.length() == 2;
    }

    public boolean isSuited() {
        return code.length() == 3 && code.charAt(2) == 's';
    }

    /** Orden de lectura: parejas, suited y offsuit; dentro de cada grupo, de mayor a menor. */
    @Override
    public int compareTo(Hand other) {
        return Integer.compare(sortKey(), other.sortKey());
    }

    @Override
    public String toString() {
        return code;
    }

    private int sortKey() {
        int group = isPair() ? 0 : isSuited() ? 1 : 2;
        return group * 1000 + RANKS.indexOf(code.charAt(0)) * 13 + RANKS.indexOf(code.charAt(1));
    }

    private static List<Hand> grid() {
        List<Hand> hands = new ArrayList<>(169);
        for (int row = 0; row < 13; row++) {
            for (int col = 0; col < 13; col++) {
                char high = RANKS.charAt(Math.min(row, col));
                char low = RANKS.charAt(Math.max(row, col));
                String code = row == col ? "" + high + low : "" + high + low + (col > row ? 's' : 'o');
                hands.add(new Hand(code));
            }
        }
        return List.copyOf(hands);
    }
}
