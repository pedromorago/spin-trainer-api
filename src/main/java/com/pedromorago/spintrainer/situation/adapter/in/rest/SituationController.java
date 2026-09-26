package com.pedromorago.spintrainer.situation.adapter.in.rest;

import com.pedromorago.spintrainer.api.SituationApi;
import com.pedromorago.spintrainer.api.model.ActionDto;
import com.pedromorago.spintrainer.api.model.PositionDto;
import com.pedromorago.spintrainer.api.model.PriorActionDto;
import com.pedromorago.spintrainer.api.model.SituationDto;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.web.Caching;
import com.pedromorago.spintrainer.situation.application.port.in.SituationCatalog;
import com.pedromorago.spintrainer.situation.domain.PriorAction;
import com.pedromorago.spintrainer.situation.domain.Situation;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SituationController implements SituationApi {

    private final SituationCatalog catalog;

    SituationController(SituationCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public ResponseEntity<List<SituationDto>> listSituations() {
        return ResponseEntity.ok()
                .cacheControl(Caching.REVALIDATE)
                .body(catalog.all().stream().map(SituationController::toDto).toList());
    }

    static SituationDto toDto(Situation situation) {
        SituationDto dto = new SituationDto(
                situation.key().value(),
                situation.label(),
                SituationDto.FormatEnum.fromValue(situation.format().code()),
                PositionDto.fromValue(situation.hero().name()),
                situation.priorActions().stream()
                        .map(SituationController::toDto)
                        .toList(),
                // uniqueItems → Set en el DTO generado; LinkedHashSet conserva el orden del catálogo.
                situation.stacks().stream().map(Stack::bigBlinds).collect(Collectors.toCollection(LinkedHashSet::new)),
                situation.actions().stream()
                        .map(action -> ActionDto.fromValue(action.code()))
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
        situation.notes().ifPresent(dto::setNotes);
        return dto;
    }

    private static PriorActionDto toDto(PriorAction prior) {
        return new PriorActionDto(
                PositionDto.fromValue(prior.position().name()),
                PriorActionDto.ActionEnum.fromValue(prior.move().name()));
    }
}
