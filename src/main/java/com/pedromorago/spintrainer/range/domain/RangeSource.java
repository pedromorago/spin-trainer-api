package com.pedromorago.spintrainer.range.domain;

/** Where a range comes from: the PDF reference (seed) or the user's custom range (ADR-0012). */
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
