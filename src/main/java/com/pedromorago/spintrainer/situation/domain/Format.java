package com.pedromorago.spintrainer.situation.domain;

/** Spin & Go format (ADR-0011). */
public enum Format {
    THREE_MAX("3max"),
    HEADS_UP("hu");

    private final String code;

    Format(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Format fromCode(String code) {
        for (Format format : values()) {
            if (format.code.equals(code)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Formato desconocido: " + code);
    }
}
