package com.pedromorago.spintrainer.range.adapter.in.rest;

import com.pedromorago.spintrainer.api.RangeApi;
import com.pedromorago.spintrainer.api.model.ActionDto;
import com.pedromorago.spintrainer.api.model.RangeDto;
import com.pedromorago.spintrainer.api.model.RangeWriteDto;
import com.pedromorago.spintrainer.range.application.port.in.ManageUserRanges;
import com.pedromorago.spintrainer.range.application.port.in.ManageUserRanges.SavedRange;
import com.pedromorago.spintrainer.range.application.port.in.ReadDefaultRanges;
import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.security.CurrentUser;
import com.pedromorago.spintrainer.shared.web.Caching;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class RangeController implements RangeApi {

    private final ReadDefaultRanges defaults;
    private final ManageUserRanges userRanges;
    private final CurrentUser currentUser;

    RangeController(ReadDefaultRanges defaults, ManageUserRanges userRanges, CurrentUser currentUser) {
        this.defaults = defaults;
        this.userRanges = userRanges;
        this.currentUser = currentUser;
    }

    @Override
    public ResponseEntity<List<RangeDto>> listDefaultRanges() {
        return ResponseEntity.ok()
                .cacheControl(Caching.REVALIDATE)
                .body(defaults.all().stream().map(RangeController::toDto).toList());
    }

    @Override
    public ResponseEntity<RangeDto> getDefaultRange(String situation, BigDecimal stack) {
        return ResponseEntity.ok()
                .cacheControl(Caching.REVALIDATE)
                .body(toDto(defaults.get(SituationKey.of(situation), Stack.of(stack))));
    }

    @Override
    public ResponseEntity<List<RangeDto>> listUserRanges() {
        return ResponseEntity.ok(userRanges.all(currentUser.id()).stream()
                .map(RangeController::toDto)
                .toList());
    }

    @Override
    public ResponseEntity<RangeDto> getUserRange(String situation, BigDecimal stack) {
        return ResponseEntity.ok(toDto(userRanges.get(currentUser.id(), SituationKey.of(situation), Stack.of(stack))));
    }

    @Override
    public ResponseEntity<RangeDto> putUserRange(String situation, BigDecimal stack, RangeWriteDto body) {
        Map<String, Action> hands = new LinkedHashMap<>();
        body.getHands().forEach((hand, action) -> hands.put(hand, Action.fromCode(action.getValue())));
        SavedRange saved = userRanges.save(
                currentUser.id(), SituationKey.of(situation), Stack.of(stack), hands, body.getVersion());
        return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(toDto(saved.range()));
    }

    @Override
    public ResponseEntity<Void> deleteUserRange(String situation, BigDecimal stack) {
        userRanges.delete(currentUser.id(), SituationKey.of(situation), Stack.of(stack));
        return ResponseEntity.noContent().build();
    }

    static RangeDto toDto(Range range) {
        Map<String, ActionDto> hands = new LinkedHashMap<>();
        range.hands().forEach((hand, action) -> hands.put(hand.code(), ActionDto.fromValue(action.code())));
        RangeDto dto = new RangeDto(
                range.situation().value(),
                range.stack().bigBlinds(),
                hands,
                RangeDto.SourceEnum.fromValue(range.source().code()),
                range.version());
        range.updatedAt().ifPresent(at -> dto.setUpdatedAt(at.atOffset(ZoneOffset.UTC)));
        return dto;
    }
}
