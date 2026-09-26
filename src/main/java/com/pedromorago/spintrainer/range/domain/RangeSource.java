package com.pedromorago.spintrainer.range.domain;

/** De dónde sale un rango: la referencia del PDF (seed) o el personalizado del usuario (ADR-0012). */
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
