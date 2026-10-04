package com.pedromorago.spintrainer.situation.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.pedromorago.spintrainer.situation.SituationFixtures;
import com.pedromorago.spintrainer.situation.domain.Situation;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The seed has no notes (ADR-0024), so the integration tests only see the field left out. */
class SituationControllerTest {

    @Test
    void notesAreMappedWhenTheSituationHasThem() {
        Situation plain = SituationFixtures.btnOpen();
        Situation annotated = new Situation(
                plain.key(),
                plain.label(),
                plain.format(),
                plain.hero(),
                plain.priorActions(),
                plain.stacks(),
                plain.actions(),
                Optional.of("Open wider against passive blinds."));

        assertThat(SituationController.toDto(annotated).getNotes()).isEqualTo("Open wider against passive blinds.");
        assertThat(SituationController.toDto(plain).getNotes()).isNull();
    }
}
