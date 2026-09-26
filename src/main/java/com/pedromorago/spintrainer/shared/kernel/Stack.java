package com.pedromorago.spintrainer.shared.kernel;

import java.math.BigDecimal;

/**
 * Effective stack in BB: from 1 to 100 in multiples of 0.5 (12.5 exists). Stored in half blinds so that {@code 25},
 * {@code 25.0} and {@code 25.00} are the same stack.
 */
public record Stack(int halfBigBlinds) implements Comparable<Stack> {

    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    public Stack {
        if (halfBigBlinds < 2 || halfBigBlinds > 200) {
            throw DomainException.validation("stack", "el stack debe estar entre 1 y 100 BB");
        }
    }

    public static Stack of(BigDecimal bigBlinds) {
        if (bigBlinds == null) {
            throw DomainException.validation("stack", "el stack es obligatorio");
        }
        BigDecimal halves = bigBlinds.multiply(TWO);
        if (halves.stripTrailingZeros().scale() > 0) {
            throw DomainException.validation("stack", "el stack debe ser múltiplo de 0,5 BB");
        }
        if (halves.compareTo(BigDecimal.valueOf(2)) < 0 || halves.compareTo(BigDecimal.valueOf(200)) > 0) {
            throw DomainException.validation("stack", "el stack debe estar entre 1 y 100 BB");
        }
        return new Stack(halves.intValueExact());
    }

    public static Stack of(double bigBlinds) {
        return of(BigDecimal.valueOf(bigBlinds));
    }

    /** Canonical form: {@code 25}, {@code 12.5} (no trailing zeros or scientific notation). */
    public BigDecimal bigBlinds() {
        return BigDecimal.valueOf(halfBigBlinds).divide(TWO);
    }

    @Override
    public int compareTo(Stack other) {
        return Integer.compare(halfBigBlinds, other.halfBigBlinds);
    }

    @Override
    public String toString() {
        return bigBlinds().toPlainString();
    }
}
