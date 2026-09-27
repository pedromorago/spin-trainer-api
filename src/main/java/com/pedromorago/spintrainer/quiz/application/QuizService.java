package com.pedromorago.spintrainer.quiz.application;

import com.pedromorago.spintrainer.quiz.application.port.in.ListAttempts;
import com.pedromorago.spintrainer.quiz.application.port.in.RecordAttempt;
import com.pedromorago.spintrainer.quiz.application.port.out.AttemptRepository;
import com.pedromorago.spintrainer.quiz.domain.QuizAttempt;
import com.pedromorago.spintrainer.range.application.port.in.ResolveEffectiveRange;
import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.DomainException;
import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import com.pedromorago.spintrainer.situation.application.port.in.SituationCatalog;
import com.pedromorago.spintrainer.situation.domain.Situation;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class QuizService implements RecordAttempt, ListAttempts {

    static final int MAX_LIMIT = 200;

    private final SituationCatalog catalog;
    private final ResolveEffectiveRange ranges;
    private final AttemptRepository attempts;
    private final Clock clock;

    QuizService(SituationCatalog catalog, ResolveEffectiveRange ranges, AttemptRepository attempts, Clock clock) {
        this.catalog = catalog;
        this.ranges = ranges;
        this.attempts = attempts;
        this.clock = clock;
    }

    @Override
    public QuizAttempt record(UserId user, SituationKey situationKey, Stack stack, String hand, Action given) {
        Situation situation = catalog.spot(situationKey, stack);
        Hand parsed = Hand.of(hand);
        if (!situation.allows(given)) {
            throw DomainException.validation("given", "action " + given.code() + " not allowed in " + situationKey);
        }
        Range range = ranges.effectiveRange(user, situationKey, stack)
                .orElseThrow(() -> DomainException.noRange(
                        "No range for " + situationKey + "@" + stack + ": the answer cannot be graded"));
        QuizAttempt attempt = QuizAttempt.grade(
                UUID.randomUUID(),
                user,
                situation,
                stack,
                parsed,
                given,
                range,
                clock.instant().truncatedTo(ChronoUnit.MILLIS));
        attempts.insert(attempt);
        return attempt;
    }

    @Override
    public Page list(
            UserId user, Optional<SituationKey> situation, Optional<Stack> stack, int limit, Optional<String> cursor) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw DomainException.validation("limit", "limit must be between 1 and " + MAX_LIMIT);
        }
        // One extra row is requested to know whether there is a next page without counting all the rows.
        List<QuizAttempt> rows =
                attempts.findPage(user, situation, stack, cursor.map(AttemptCursor::decode), limit + 1);
        if (rows.size() <= limit) {
            return new Page(rows, Optional.empty());
        }
        List<QuizAttempt> page = rows.subList(0, limit);
        return new Page(List.copyOf(page), Optional.of(AttemptCursor.encode(page.getLast())));
    }
}
