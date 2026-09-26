-- Intentos del Quiz: eventos inmutables (ADR-0007). El servidor los corrige y guarda la acción esperada, el rango
-- y su versión de ese momento (ADR-0013); las estadísticas son consultas sobre esta tabla.

CREATE TABLE app.quiz_attempt (
    id            uuid          PRIMARY KEY,
    user_id       uuid          NOT NULL,
    situation     text          NOT NULL,
    stack         numeric(4, 1) NOT NULL,
    hand          text          NOT NULL REFERENCES app.hand,
    given         text          NOT NULL,
    expected      text          NOT NULL,
    correct       boolean       NOT NULL,
    range_source  text          NOT NULL CHECK (range_source IN ('default', 'user')),
    range_version integer       NOT NULL CHECK (range_version >= 1),
    answered_at   timestamptz   NOT NULL,
    CHECK (correct = (given = expected)),
    FOREIGN KEY (situation, stack) REFERENCES app.situation_stack,
    FOREIGN KEY (situation, given) REFERENCES app.situation_action (situation, action),
    FOREIGN KEY (situation, expected) REFERENCES app.situation_action (situation, action)
);

-- Paginación por clave del más reciente al más antiguo, y agregados por mano.
CREATE INDEX quiz_attempt_recent ON app.quiz_attempt (user_id, answered_at DESC, id DESC);
CREATE INDEX quiz_attempt_by_hand ON app.quiz_attempt (user_id, situation, stack, hand);

-- Inmutables por permisos, no solo por código: la API inserta y lee; no puede modificar ni borrar.
GRANT SELECT, INSERT ON app.quiz_attempt TO ${app_role};
