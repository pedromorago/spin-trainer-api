package com.pedromorago.spintrainer.situation.application;

import com.pedromorago.spintrainer.shared.kernel.DomainException;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.situation.application.port.in.SituationCatalog;
import com.pedromorago.spintrainer.situation.application.port.out.SituationRepository;
import com.pedromorago.spintrainer.situation.domain.Situation;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * El catálogo solo cambia con una migración de seed (y una migración implica un despliegue), así que se lee una vez
 * y se sirve desde memoria: cada intento del Quiz lo consulta.
 */
@Service
class SituationCatalogService implements SituationCatalog {

    private final SituationRepository repository;
    private volatile Catalog catalog;

    SituationCatalogService(SituationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Situation> all() {
        return catalog().situations();
    }

    @Override
    public Situation spot(SituationKey key, Stack stack) {
        Situation situation = catalog().byKey().get(key);
        if (situation == null || !situation.hasStack(stack)) {
            throw DomainException.notFound("Situación/stack desconocido: " + key + "@" + stack);
        }
        return situation;
    }

    private Catalog catalog() {
        Catalog current = catalog;
        if (current == null) {
            List<Situation> situations = repository.findAll();
            current = new Catalog(
                    situations,
                    situations.stream().collect(Collectors.toUnmodifiableMap(Situation::key, Function.identity())));
            catalog = current;
        }
        return current;
    }

    private record Catalog(List<Situation> situations, Map<SituationKey, Situation> byKey) {}
}
