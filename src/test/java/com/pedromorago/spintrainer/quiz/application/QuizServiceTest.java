package com.pedromorago.spintrainer.quiz.application;

import static com.pedromorago.spintrainer.situation.SituationFixtures.btnOpen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pedromorago.spintrainer.quiz.application.port.in.ListAttempts.Page;
import com.pedromorago.spintrainer.quiz.application.port.out.AttemptRepository;
import com.pedromorago.spintrainer.quiz.domain.QuizAttempt;
import com.pedromorago.spintrainer.range.application.port.in.ResolveEffectiveRange;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class QuizServiceTest {

    static final SituationKey BTN_OPEN = SituationKey.of("btn_open");
    static final Stack BB25 = Stack.of(25);

    final UserId user = new UserId(UUID.randomUUID());
    final Map<Stack, Range> effective = new HashMap<>();
    final List<QuizAttempt> stored = new ArrayList<>();

    final SituationCatalog catalog = new SituationCatalog() {
        @Override
        public List<Situation> all() {
            return List.of(btnOpen());
        }

        @Override
        public Situation spot(SituationKey key, Stack stack) {
            if (!key.equals(BTN_OPEN) || !btnOpen().hasStack(stack)) {
                throw DomainException.notFound("Situación/stack desconocido");
            }
            return btnOpen();
        }
    };
    final ResolveEffectiveRange ranges = (u, situation, stack) -> Optional.ofNullable(effective.get(stack));
    final AttemptRepository attempts = new AttemptRepository() {
        @Override
        public void insert(QuizAttempt attempt) {
            stored.add(attempt);
        }

        @Override
        public List<QuizAttempt> findPage(
                UserId u, Optional<SituationKey> s, Optional<Stack> st, Optional<Position> after, int limit) {
            Comparator<QuizAttempt> newestFirst = Comparator.comparing(QuizAttempt::answeredAt)
                    .thenComparing(QuizAttempt::id)
                    .reversed();
            return stored.stream()
                    .sorted(newestFirst)
                    .filter(a -> after.map(p -> a.answeredAt().isBefore(p.answeredAt())
                                    || (a.answeredAt().equals(p.answeredAt()) && a.id().compareTo(p.id()) < 0))
                            .orElse(true))
                    .limit(limit)
                    .toList();
        }
    };
    final QuizService service = new QuizService(
            catalog, ranges, attempts, Clock.fixed(Instant.parse("2026-09-26T10:00:00.987654Z"), ZoneOffset.UTC));

    void referenceRange() {
        effective.put(
                BB25,
                new Range(
                        BTN_OPEN,
                        BB25,
                        Map.of(Hand.of("AA"), Action.MR_4B_C),
                        RangeSource.DEFAULT,
                        7,
                        Optional.empty()));
    }

    @Test
    void gradesAgainstTheEffectiveRangeAndStoresTheEvent() {
        referenceRange();

        QuizAttempt attempt = service.record(user, BTN_OPEN, BB25, "AA", Action.MR_C_C);

        assertThat(attempt.expected()).isEqualTo(Action.MR_4B_C);
        assertThat(attempt.correct()).isFalse();
        assertThat(attempt.rangeVersion()).isEqualTo(7);
        assertThat(attempt.answeredAt()).isEqualTo(Instant.parse("2026-09-26T10:00:00.987Z"));
        assertThat(stored).containsExactly(attempt);
    }

    @Test
    void withoutARangeThereIsNothingToGradeAgainst() {
        assertKind(() -> service.record(user, BTN_OPEN, BB25, "AA", Action.FOLD), Kind.NO_RANGE);
        assertThat(stored).isEmpty();
    }

    @Test
    void validatesInTheSameOrderAsTheMock() {
        referenceRange();

        assertKind(() -> service.record(user, BTN_OPEN, Stack.of(12.5), "AA", Action.FOLD), Kind.NOT_FOUND);
        assertKind(() -> service.record(user, BTN_OPEN, BB25, "KAs", Action.FOLD), Kind.VALIDATION);
        assertKind(() -> service.record(user, BTN_OPEN, Stack.of(20), "AA", Action.CHECK), Kind.VALIDATION);
        assertKind(() -> service.record(user, BTN_OPEN, Stack.of(20), "AA", Action.FOLD), Kind.NO_RANGE);
    }

    @Test
    void pagesFromNewestToOldestWithACursorUntilTheEnd() {
        referenceRange();
        for (int i = 0; i < 5; i++) {
            attempts.insert(QuizAttempt.grade(
                    UUID.randomUUID(),
                    user,
                    btnOpen(),
                    BB25,
                    Hand.of("AA"),
                    Action.FOLD,
                    effective.get(BB25),
                    Instant.parse("2026-09-26T10:00:00Z").plusSeconds(i)));
        }

        Page first = service.list(user, Optional.empty(), Optional.empty(), 2, Optional.empty());
        Page second = service.list(user, Optional.empty(), Optional.empty(), 2, first.nextCursor());
        Page third = service.list(user, Optional.empty(), Optional.empty(), 2, second.nextCursor());

        assertThat(first.items())
                .extracting(QuizAttempt::answeredAt)
                .containsExactly(Instant.parse("2026-09-26T10:00:04Z"), Instant.parse("2026-09-26T10:00:03Z"));
        assertThat(second.items())
                .extracting(QuizAttempt::answeredAt)
                .containsExactly(Instant.parse("2026-09-26T10:00:02Z"), Instant.parse("2026-09-26T10:00:01Z"));
        assertThat(third.items())
                .extracting(QuizAttempt::answeredAt)
                .containsExactly(Instant.parse("2026-09-26T10:00:00Z"));
        assertThat(third.nextCursor()).isEmpty();
    }

    @Test
    void anExactlyFullLastPageHasNoNextCursor() {
        referenceRange();
        for (int i = 0; i < 3; i++) {
            attempts.insert(QuizAttempt.grade(
                    UUID.randomUUID(),
                    user,
                    btnOpen(),
                    BB25,
                    Hand.of("AA"),
                    Action.FOLD,
                    effective.get(BB25),
                    Instant.parse("2026-09-26T10:00:00Z").plusSeconds(i)));
        }

        Page page = service.list(user, Optional.empty(), Optional.empty(), 3, Optional.empty());

        assertThat(page.items()).hasSize(3);
        assertThat(page.nextCursor()).isEmpty();
    }

    /** Boundary values: 1 and 200 are valid, 0 and 201 are not. */
    @Test
    void theLimitIsBetweenOneAnd200() {
        assertKind(() -> service.list(user, Optional.empty(), Optional.empty(), 0, Optional.empty()), Kind.VALIDATION);
        assertKind(
                () -> service.list(user, Optional.empty(), Optional.empty(), 201, Optional.empty()), Kind.VALIDATION);
        assertThatCode(() -> service.list(user, Optional.empty(), Optional.empty(), 1, Optional.empty()))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.list(user, Optional.empty(), Optional.empty(), 200, Optional.empty()))
                .doesNotThrowAnyException();
    }

    private static void assertKind(ThrowingCallable call, Kind kind) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(
                        DomainException.class, e -> assertThat(e.kind()).isEqualTo(kind));
    }
}
