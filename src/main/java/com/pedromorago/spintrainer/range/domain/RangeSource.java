package com.pedromorago.spintrainer.range.domain;

/** Where a range comes from: the reference one (seed, ADR-0024) or the user's custom range (ADR-0012). */
public enum RangeSource {
    DEFAULT("default"),
    USER("user");

    private final String code;

    RangeSource(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
