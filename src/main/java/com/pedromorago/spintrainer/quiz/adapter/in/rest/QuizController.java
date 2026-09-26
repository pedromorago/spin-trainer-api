package com.pedromorago.spintrainer.quiz.adapter.in.rest;

import com.pedromorago.spintrainer.api.QuizApi;
import com.pedromorago.spintrainer.api.model.ActionDto;
import com.pedromorago.spintrainer.api.model.AttemptDto;
import com.pedromorago.spintrainer.api.model.AttemptPageDto;
import com.pedromorago.spintrainer.api.model.AttemptWriteDto;
import com.pedromorago.spintrainer.quiz.application.port.in.ListAttempts;
import com.pedromorago.spintrainer.quiz.application.port.in.ListAttempts.Page;
import com.pedromorago.spintrainer.quiz.application.port.in.RecordAttempt;
import com.pedromorago.spintrainer.quiz.domain.QuizAttempt;
import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.security.CurrentUser;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class QuizController implements QuizApi {

    private final RecordAttempt recordAttempt;
    private final ListAttempts listAttempts;
    private final CurrentUser currentUser;

    QuizController(RecordAttempt recordAttempt, ListAttempts listAttempts, CurrentUser currentUser) {
        this.recordAttempt = recordAttempt;
        this.listAttempts = listAttempts;
        this.currentUser = currentUser;
    }

    @Override
    public ResponseEntity<AttemptDto> recordAttempt(AttemptWriteDto body) {
        QuizAttempt attempt = recordAttempt.record(
                currentUser.id(),
                SituationKey.of(body.getSituation()),
                Stack.of(body.getStack()),
                body.getHand(),
                Action.fromCode(body.getGiven().getValue()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(attempt));
    }

    @Override
    public ResponseEntity<AttemptPageDto> listAttempts(
            Integer limit, @Nullable String cursor, @Nullable String situation, @Nullable BigDecimal stack) {
        Page page = listAttempts.list(
                currentUser.id(),
                Optional.ofNullable(situation).map(SituationKey::of),
                Optional.ofNullable(stack).map(Stack::of),
                limit,
                Optional.ofNullable(cursor));
        return ResponseEntity.ok(new AttemptPageDto(
                page.items().stream().map(QuizController::toDto).toList(),
                page.nextCursor().orElse(null)));
    }

    static AttemptDto toDto(QuizAttempt attempt) {
        return new AttemptDto(
                attempt.id(),
                attempt.situation().value(),
                attempt.stack().bigBlinds(),
                attempt.hand().code(),
                ActionDto.fromValue(attempt.given().code()),
                ActionDto.fromValue(attempt.expected().code()),
                attempt.correct(),
                AttemptDto.RangeSourceEnum.fromValue(attempt.rangeSource().code()),
                attempt.rangeVersion(),
                attempt.answeredAt().atOffset(ZoneOffset.UTC));
    }
}
