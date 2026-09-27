package com.pedromorago.spintrainer.shared.kernel;

import java.util.regex.Pattern;

/** Key of a catalog situation ({@code btn_open}, {@code hu_bb_vs_os}...). */
public record SituationKey(String value) {

    private static final Pattern FORMAT = Pattern.compile("[a-z0-9_]{1,64}");

    public SituationKey {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw DomainException.validation("situation", "invalid situation key");
        }
    }

    public static SituationKey of(String value) {
        return new SituationKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
