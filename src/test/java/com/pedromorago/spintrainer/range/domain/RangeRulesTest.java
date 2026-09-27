package com.pedromorago.spintrainer.range.domain;

import static com.pedromorago.spintrainer.situation.SituationFixtures.bbVsSbLimp;
import static com.pedromorago.spintrainer.situation.SituationFixtures.btnOpen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.DomainException;
import com.pedromorago.spintrainer.shared.kernel.DomainException.FieldError;
import com.pedromorago.spintrainer.shared.kernel.Hand;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RangeRulesTest {

    @Test
    void anExplicitHandHasItsAction() {
        assertThat(RangeRules.actionFor(Map.of(Hand.of("AA"), Action.MR_4B_C), Hand.of("AA"), btnOpen()))
                .isEqualTo(Action.MR_4B_C);
    }

    @Test
    void anAbsentHandHasTheImplicitAction() {
        assertThat(RangeRules.actionFor(Map.of(), Hand.of("72o"), btnOpen())).isEqualTo(Action.FOLD);
        assertThat(RangeRules.actionFor(Map.of(), Hand.of("72o"), bbVsSbLimp())).isEqualTo(Action.CHECK);
    }

    @Test
    void normalizingDropsTheImplicitActionAndSortsHands() {
        Map<String, Action> hands = new LinkedHashMap<>();
        hands.put("72o", Action.FOLD);
        hands.put("KK", Action.MR_4B_C);
        hands.put("AA", Action.MR_4B_C);

        assertThat(RangeRules.normalize(hands, btnOpen()))
                .containsExactly(Map.entry(Hand.of("AA"), Action.MR_4B_C), Map.entry(Hand.of("KK"), Action.MR_4B_C));
    }

    @Test
    void checkIsImplicitWhereFoldIsNotAnOption() {
        assertThat(RangeRules.normalize(Map.of("72o", Action.CHECK, "AA", Action.ALLIN), bbVsSbLimp()))
                .containsOnlyKeys(Hand.of("AA"));
    }

    @Test
    void reportsEveryInvalidEntryAtOnce() {
        Map<String, Action> hands = new LinkedHashMap<>();
        hands.put("AAs", Action.MR_4B_C);
        hands.put("KAs", Action.MR_4B_C);
        hands.put("QQ", Action.CHECK);
        hands.put("JJ", Action.MR_C_C);

        assertThatThrownBy(() -> RangeRules.normalize(hands, btnOpen()))
                .isInstanceOfSatisfying(DomainException.class, e -> {
                    assertThat(e.kind()).isEqualTo(DomainException.Kind.VALIDATION);
                    assertThat(e.getMessage()).isEqualTo("3 invalid entries in hands");
                    assertThat(e.errors())
                            .containsExactly(
                                    new FieldError("hands.AAs", "invalid hand"),
                                    new FieldError("hands.KAs", "invalid hand"),
                                    new FieldError("hands.QQ", "action CHECK not allowed in btn_open"));
                });
    }
}
