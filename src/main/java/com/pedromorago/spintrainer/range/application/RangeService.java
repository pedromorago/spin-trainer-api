package com.pedromorago.spintrainer.range.application;

import com.pedromorago.spintrainer.range.application.port.in.ManageUserRanges;
import com.pedromorago.spintrainer.range.application.port.in.ReadDefaultRanges;
import com.pedromorago.spintrainer.range.application.port.in.ResolveEffectiveRange;
import com.pedromorago.spintrainer.range.application.port.out.DefaultRangeRepository;
import com.pedromorago.spintrainer.range.application.port.out.UserRangeRepository;
import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.range.domain.RangeRules;
import com.pedromorago.spintrainer.range.domain.RangeSource;
import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.DomainException;
import com.pedromorago.spintrainer.shared.kernel.Hand;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import com.pedromorago.spintrainer.situation.application.port.in.SituationCatalog;
import com.pedromorago.spintrainer.situation.domain.Situation;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RangeService implements ReadDefaultRanges, ManageUserRanges, ResolveEffectiveRange {

    private final SituationCatalog catalog;
    private final DefaultRangeRepository defaults;
    private final UserRangeRepository userRanges;
    private final Clock clock;

    RangeService(
            SituationCatalog catalog, DefaultRangeRepository defaults, UserRangeRepository userRanges, Clock clock) {
        this.catalog = catalog;
        this.defaults = defaults;
        this.userRanges = userRanges;
        this.clock = clock;
    }

    @Override
    public List<Range> all() {
        return defaults.findAll();
    }

    @Override
    public Range get(SituationKey situation, Stack stack) {
        catalog.spot(situation, stack);
        return defaults.find(situation, stack)
                .orElseThrow(() -> DomainException.notFound("Sin rango de referencia para " + situation + "@" + stack));
    }

    @Override
    public List<Range> all(UserId user) {
        return userRanges.findAll(user);
    }

    @Override
    public Range get(UserId user, SituationKey situation, Stack stack) {
        catalog.spot(situation, stack);
        return userRanges
                .find(user, situation, stack)
                .orElseThrow(() -> DomainException.notFound("Sin rango personalizado para " + situation + "@" + stack));
    }

    @Override
    @Transactional
    public SavedRange save(UserId user, SituationKey situation, Stack stack, Map<String, Action> hands, int version) {
        Situation spot = catalog.spot(situation, stack);
        if (version < 0) {
            throw DomainException.validation("version", "la versión debe ser un entero ≥ 0");
        }
        Map<Hand, Action> normalized = RangeRules.normalize(hands, spot);
        // Milisegundos, como el navegador: lo que devuelve el PUT es exactamente lo que devolverá un GET posterior.
        Instant now = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        Range next = new Range(situation, stack, normalized, RangeSource.USER, version + 1, Optional.of(now));
        boolean saved = version == 0 ? userRanges.insert(user, next) : userRanges.replace(user, next, version);
        if (!saved) {
            throw conflict(user, situation, stack);
        }
        return new SavedRange(next, version == 0);
    }

    @Override
    @Transactional
    public void delete(UserId user, SituationKey situation, Stack stack) {
        catalog.spot(situation, stack);
        userRanges.delete(user, situation, stack);
    }

    @Override
    public Optional<Range> effectiveRange(UserId user, SituationKey situation, Stack stack) {
        return userRanges.find(user, situation, stack).or(() -> defaults.find(situation, stack));
    }

    private DomainException conflict(UserId user, SituationKey situation, Stack stack) {
        return userRanges
                .find(user, situation, stack)
                .map(current ->
                        DomainException.conflict("El rango está en la versión " + current.version() + "; recarga"))
                .orElseGet(() -> DomainException.conflict("El rango personalizado ya no existe; recarga"));
    }
}
