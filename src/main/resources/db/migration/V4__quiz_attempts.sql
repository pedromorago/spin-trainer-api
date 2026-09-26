-- Quiz attempts: immutable events (ADR-0007). The server grades them and stores the expected action, the range
-- and its version at that moment (ADR-0013); the statistics are queries over this table.

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

-- Keyset pagination from most recent to oldest, and per-hand aggregates.
CREATE INDEX quiz_attempt_recent ON app.quiz_attempt (user_id, answered_at DESC, id DESC);
CREATE INDEX quiz_attempt_by_hand ON app.quiz_attempt (user_id, situation, stack, hand);

-- Immutable through permissions, not just through code: the API inserts and reads; it cannot update or delete.
GRANT SELECT, INSERT ON app.quiz_attempt TO ${app_role};
