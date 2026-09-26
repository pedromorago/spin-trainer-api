package com.pedromorago.spintrainer.range.application;

import com.pedromorago.spintrainer.range.application.port.out.DefaultRangeRepository;
import com.pedromorago.spintrainer.range.application.port.out.UserRangeRepository;
import com.pedromorago.spintrainer.range.domain.Range;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.shared.kernel.UserId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Repositorios en memoria con la misma semántica que los JDBC (los prueban los tests de integración). */
final class InMemoryRanges implements DefaultRangeRepository, UserRangeRepository {

    private record Key(UserId user, SituationKey situation, Stack stack) {}

    private final List<Range> defaults = new ArrayList<>();
    private final Map<Key, Range> userRanges = new LinkedHashMap<>();

    void addDefault(Range range) {
        defaults.add(range);
    }

    @Override
    public List<Range> findAll() {
        return List.copyOf(defaults);
    }

    @Override
    public Optional<Range> find(SituationKey situation, Stack stack) {
        return defaults.stream()
                .filter(r -> r.situation().equals(situation) && r.stack().equals(stack))
                .findFirst();
    }

    @Override
    public List<Range> findAll(UserId user) {
        return userRanges.entrySet().stream()
                .filter(e -> e.getKey().user().equals(user))
                .map(Map.Entry::getValue)
                .toList();
    }

    @Override
    public Optional<Range> find(UserId user, SituationKey situation, Stack stack) {
        return Optional.ofNullable(userRanges.get(new Key(user, situation, stack)));
    }

    @Override
    public boolean insert(UserId user, Range range) {
        return userRanges.putIfAbsent(new Key(user, range.situation(), range.stack()), range) == null;
    }

    @Override
    public boolean replace(UserId user, Range range, int expectedVersion) {
        Key key = new Key(user, range.situation(), range.stack());
        Range current = userRanges.get(key);
        if (current == null || current.version() != expectedVersion) {
            return false;
        }
        userRanges.put(key, range);
        return true;
    }

    @Override
    public void delete(UserId user, SituationKey situation, Stack stack) {
        userRanges.remove(new Key(user, situation, stack));
    }
}
