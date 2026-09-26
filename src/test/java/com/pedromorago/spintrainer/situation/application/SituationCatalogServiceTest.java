package com.pedromorago.spintrainer.situation.application;

import static com.pedromorago.spintrainer.situation.SituationFixtures.bbVsSbLimp;
import static com.pedromorago.spintrainer.situation.SituationFixtures.btnOpen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pedromorago.spintrainer.shared.kernel.DomainException;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.situation.application.port.out.SituationRepository;
import com.pedromorago.spintrainer.situation.domain.Situation;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SituationCatalogServiceTest {

    final AtomicInteger loads = new AtomicInteger();
    final SituationRepository repository = () -> {
        loads.incrementAndGet();
        return List.of(btnOpen(), bbVsSbLimp());
    };
    final SituationCatalogService catalog = new SituationCatalogService(repository);

    @Test
    void keepsThePresentationOrder() {
        assertThat(catalog.all())
                .extracting(Situation::key)
                .extracting(SituationKey::value)
                .containsExactly("btn_open", "bb_vs_sb_limp");
    }

    @Test
    void findsAnExistingSpot() {
        assertThat(catalog.spot(SituationKey.of("bb_vs_sb_limp"), Stack.of(10))).isEqualTo(bbVsSbLimp());
    }

    @Test
    void anUnknownSituationOrStackIsNotFound() {
        assertThatThrownBy(() -> catalog.spot(SituationKey.of("mtt_open"), Stack.of(25)))
                .isInstanceOfSatisfying(
                        DomainException.class, e -> assertThat(e.kind()).isEqualTo(DomainException.Kind.NOT_FOUND));
        assertThatThrownBy(() -> catalog.spot(SituationKey.of("bb_vs_sb_limp"), Stack.of(8)))
                .isInstanceOfSatisfying(
                        DomainException.class,
                        e -> assertThat(e.getMessage()).isEqualTo("Situación/stack desconocido: bb_vs_sb_limp@8"));
    }

    @Test
    void readsTheCatalogOnlyOnce() {
        catalog.all();
        catalog.spot(SituationKey.of("btn_open"), Stack.of(25));
        catalog.all();

        assertThat(loads).hasValue(1);
    }
}
