package com.pedromorago.spintrainer.situation.adapter.out.persistence;

import com.pedromorago.spintrainer.shared.kernel.Action;
import com.pedromorago.spintrainer.shared.kernel.SituationKey;
import com.pedromorago.spintrainer.shared.kernel.Stack;
import com.pedromorago.spintrainer.situation.application.port.out.SituationRepository;
import com.pedromorago.spintrainer.situation.domain.Format;
import com.pedromorago.spintrainer.situation.domain.Position;
import com.pedromorago.spintrainer.situation.domain.PriorAction;
import com.pedromorago.spintrainer.situation.domain.Situation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcSituationRepository implements SituationRepository {

    private final JdbcClient jdbc;

    JdbcSituationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Situation> findAll() {
        Map<String, List<PriorAction>> priorActions = jdbc
                .sql("SELECT situation, position, action FROM app.situation_prior_action ORDER BY situation, seq")
                .query((rs, row) -> Map.entry(
                        rs.getString("situation"),
                        new PriorAction(
                                Position.valueOf(rs.getString("position")),
                                PriorAction.Move.valueOf(rs.getString("action")))))
                .list()
                .stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        Map<String, List<Stack>> stacks = jdbc
                .sql("SELECT situation, stack FROM app.situation_stack ORDER BY situation, stack DESC")
                .query((rs, row) -> Map.entry(rs.getString("situation"), Stack.of(rs.getBigDecimal("stack"))))
                .list()
                .stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        Map<String, List<Action>> actions = jdbc
                .sql("SELECT situation, action FROM app.situation_action ORDER BY situation, seq")
                .query((rs, row) -> Map.entry(rs.getString("situation"), Action.fromCode(rs.getString("action"))))
                .list()
                .stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        return jdbc.sql("SELECT key, label, format, hero, notes FROM app.situation ORDER BY position")
                .query((rs, row) -> {
                    String key = rs.getString("key");
                    return new Situation(
                            SituationKey.of(key),
                            rs.getString("label"),
                            Format.fromCode(rs.getString("format")),
                            Position.valueOf(rs.getString("hero")),
                            priorActions.getOrDefault(key, List.of()),
                            stacks.getOrDefault(key, List.of()),
                            actions.getOrDefault(key, List.of()),
                            Optional.ofNullable(rs.getString("notes")));
                })
                .list();
    }
}
