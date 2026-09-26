package com.pedromorago.spintrainer.range.application;

import static com.pedromorago.spintrainer.situation.SituationFixtures.btnOpen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pedromorago.spintrainer.range.application.port.in.ManageUserRanges.SavedRange;
import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.range.domain.RangeSource;
import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.DomainException;
import com.pedromorago.spintrainer.shared.kernel.DomainException.Kind;
import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import com.pedromorago.spintrainer.situation.application.port.in.SituationCatalog;
import com.pedromorago.spintrainer.situation.domain.Situation;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class RangeServiceTest {

    static final Instant NOW = Instant.parse("2026-09-26T10:00:00.123456789Z");
    static final SituationKey BTN_OPEN = SituationKey.of("btn_open");
    static final Stack BB25 = Stack.of(25);

    final UserId pedro = new UserId(UUID.randomUUID());
    final UserId other = new UserId(UUID.randomUUID());
    final InMemoryRanges ranges = new InMemoryRanges();
    final SituationCatalog catalog = new SituationCatalog() {
        @Override
        public List<Situation> all() {
            return List.of(btnOpen());
        }

        @Override
        public Situation spot(SituationKey key, Stack stack) {
            if (!key.equals(BTN_OPEN) || !btnOpen().hasStack(stack)) {
                throw DomainException.notFound("Situación/stack desconocido: " + key + "@" + stack);
            }
            return btnOpen();
        }
    };
    final RangeService service = new RangeService(catalog, ranges, ranges, Clock.fixed(NOW, ZoneOffset.UTC));

    final Range reference =
            new Range(BTN_OPEN, BB25, Map.of(Hand.of("AA"), Action.MR_4B_C), RangeSource.DEFAULT, 3, Optional.empty());

    @Test
    void creatingWithVersionZeroGivesVersionOne() {
        SavedRange saved = service.save(pedro, BTN_OPEN, BB25, Map.of("AA", Action.MR_C_C, "72o", Action.FOLD), 0);

        assertThat(saved.created()).isTrue();
        assertThat(saved.range().version()).isEqualTo(1);
        assertThat(saved.range().source()).isEqualTo(RangeSource.USER);
        assertThat(saved.range().updatedAt()).contains(Instant.parse("2026-09-26T10:00:00.123Z"));
        assertThat(saved.range().hands()).containsExactly(Map.entry(Hand.of("AA"), Action.MR_C_C));
    }

    @Test
    void replacingTheCurrentVersionIncrementsIt() {
        service.save(pedro, BTN_OPEN, BB25, Map.of("AA", Action.MR_C_C), 0);

        SavedRange saved = service.save(pedro, BTN_OPEN, BB25, Map.of("KK", Action.MR_4B_C), 1);

        assertThat(saved.created()).isFalse();
        assertThat(saved.range().version()).isEqualTo(2);
        assertThat(service.get(pedro, BTN_OPEN, BB25).hands()).containsOnlyKeys(Hand.of("KK"));
    }

    @Test
    void aStaleVersionIsAConflictThatSaysTheCurrentOne() {
        service.save(pedro, BTN_OPEN, BB25, Map.of(), 0);
        service.save(pedro, BTN_OPEN, BB25, Map.of(), 1);

        assertConflict(
                () -> service.save(pedro, BTN_OPEN, BB25, Map.of(), 1), "El rango está en la versión 2; recarga");
        assertConflict(
                () -> service.save(pedro, BTN_OPEN, BB25, Map.of(), 0), "El rango está en la versión 2; recarga");
    }

    @Test
    void replacingADeletedRangeIsAConflict() {
        service.save(pedro, BTN_OPEN, BB25, Map.of(), 0);
        service.delete(pedro, BTN_OPEN, BB25);

        assertConflict(
                () -> service.save(pedro, BTN_OPEN, BB25, Map.of(), 1), "El rango personalizado ya no existe; recarga");
    }

    @Test
    void validatesTheSpotTheVersionAndTheHands() {
        assertKind(() -> service.save(pedro, SituationKey.of("mtt"), BB25, Map.of(), 0), Kind.NOT_FOUND);
        assertKind(() -> service.save(pedro, BTN_OPEN, Stack.of(12.5), Map.of(), 0), Kind.NOT_FOUND);
        assertKind(() -> service.save(pedro, BTN_OPEN, BB25, Map.of(), -1), Kind.VALIDATION);
        assertKind(() -> service.save(pedro, BTN_OPEN, BB25, Map.of("AAs", Action.FOLD), 0), Kind.VALIDATION);
    }

    @Test
    void eachUserOnlySeesTheirRanges() {
        service.save(pedro, BTN_OPEN, BB25, Map.of("AA", Action.MR_C_C), 0);

        assertThat(service.all(pedro)).hasSize(1);
        assertThat(service.all(other)).isEmpty();
        assertKind(() -> service.get(other, BTN_OPEN, BB25), Kind.NOT_FOUND);
    }

    @Test
    void deletingIsIdempotentButTheSpotMustExist() {
        service.delete(pedro, BTN_OPEN, BB25);
        service.delete(pedro, BTN_OPEN, BB25);

        assertKind(() -> service.delete(pedro, BTN_OPEN, Stack.of(30)), Kind.NOT_FOUND);
    }

    @Test
    void theEffectiveRangeIsTheUserOneIfPresentOtherwiseTheReference() {
        ranges.addDefault(reference);

        assertThat(service.effectiveRange(pedro, BTN_OPEN, BB25)).contains(reference);

        service.save(pedro, BTN_OPEN, BB25, Map.of("KK", Action.MR_4B_C), 0);
        assertThat(service.effectiveRange(pedro, BTN_OPEN, BB25))
                .get()
                .extracting(Range::source)
                .isEqualTo(RangeSource.USER);
        assertThat(service.effectiveRange(other, BTN_OPEN, BB25)).contains(reference);
        assertThat(service.effectiveRange(pedro, BTN_OPEN, Stack.of(20))).isEmpty();
    }

    @Test
    void referenceRangesAreReadOnlyAndNotFoundWhenMissing() {
        ranges.addDefault(reference);

        assertThat(service.all()).containsExactly(reference);
        assertThat(service.get(BTN_OPEN, BB25)).isEqualTo(reference);
        assertKind(() -> service.get(BTN_OPEN, Stack.of(20)), Kind.NOT_FOUND);
    }

    private static void assertConflict(ThrowingCallable call, String message) {
        assertThatThrownBy(call).isInstanceOfSatisfying(DomainException.class, e -> {
            assertThat(e.kind()).isEqualTo(Kind.CONFLICT);
            assertThat(e.getMessage()).isEqualTo(message);
        });
    }

    private static void assertKind(ThrowingCallable call, Kind kind) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(
                        DomainException.class, e -> assertThat(e.kind()).isEqualTo(kind));
    }
}
