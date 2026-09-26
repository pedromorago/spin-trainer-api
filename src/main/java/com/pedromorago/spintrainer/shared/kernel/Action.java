package com.pedromorago.spintrainer.shared.kernel;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Acción compuesta de una mano (una por mano y situación/stack). {@link #code()} es el valor del contrato ({@code 3BET});
 * el nombre Java no puede empezar por un dígito ({@code THREE_BET}).
 */
public enum Action {
    MR_4B_C("MR_4B_C"),
    MR_C_C("MR_C_C"),
    MR_C_F("MR_C_F"),
    MR_F_F("MR_F_F"),
    L_C_C("L_C_C"),
    L_C_F("L_C_F"),
    L_PUSH("L_PUSH"),
    L_F("L_F"),
    ALLIN("ALLIN"),
    THREE_BET("3BET"),
    THREE_BET_C("3BET_C"),
    CALL("CALL"),
    CALL_VS_X2("CALL_VS_X2"),
    ISO_C("ISO_C"),
    ISO_F("ISO_F"),
    LIMP("LIMP"),
    CHECK("CHECK"),
    FOLD("FOLD");

    private static final Map<String, Action> BY_CODE =
            Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(Action::code, Function.identity()));

    private final String code;

    Action(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Action fromCode(String code) {
        Action action = BY_CODE.get(code);
        if (action == null) {
            throw new IllegalArgumentException("Acción desconocida: " + code);
        }
        return action;
    }
}
