-- Catalog: the 13×13 grid hands and the Spin & Go situations (ADR-0011) with their stacks, actions and prior actions.
-- Flyway runs as spin_migrator, owner of the `app` schema; the API connects as ${app_role} (ADR-0015).

REVOKE ALL ON SCHEMA app FROM PUBLIC;
GRANT USAGE ON SCHEMA app TO ${app_role};

-- The 169 canonical hands: pair (AA), high + low + s/o (AKs, T9o). The hand tables reference it,
-- so the database rejects "AAs", "AK" or "KAs" just like the domain.
CREATE TABLE app.hand (
    code text PRIMARY KEY
);

INSERT INTO app.hand (code)
SELECT CASE
           WHEN r = c THEN substr(ranks, r, 1) || substr(ranks, c, 1)
           WHEN r < c THEN substr(ranks, r, 1) || substr(ranks, c, 1) || 's'
           ELSE substr(ranks, c, 1) || substr(ranks, r, 1) || 'o'
       END
FROM (SELECT 'AKQJT98765432' AS ranks) AS k,
     generate_series(1, 13) AS r,
     generate_series(1, 13) AS c;

CREATE TABLE app.situation (
    key      text PRIMARY KEY CHECK (key ~ '^[a-z0-9_]+$' AND length(key) <= 64),
    position smallint NOT NULL UNIQUE CHECK (position >= 1),
    label    text NOT NULL,
    format   text NOT NULL CHECK (format IN ('3max', 'hu')),
    hero     text NOT NULL CHECK (hero IN ('BTN', 'SB', 'BB')),
    notes    text
);

-- What the previous players did, in order; empty if the hero opens.
CREATE TABLE app.situation_prior_action (
    situation text     NOT NULL REFERENCES app.situation,
    seq       smallint NOT NULL CHECK (seq >= 1),
    position  text     NOT NULL CHECK (position IN ('BTN', 'SB', 'BB')),
    action    text     NOT NULL CHECK (action IN ('FOLD', 'LIMP', 'MIN_RAISE', 'RAISE', 'THREE_BET', 'CALL', 'SHOVE')),
    PRIMARY KEY (situation, seq)
);

-- Effective stacks in BB, multiples of 0.5 (12.5 exists).
CREATE TABLE app.situation_stack (
    situation text         NOT NULL REFERENCES app.situation,
    stack     numeric(4, 1) NOT NULL CHECK (stack BETWEEN 1 AND 100 AND stack * 2 = trunc(stack * 2)),
    PRIMARY KEY (situation, stack)
);

-- Possible actions of each situation, in palette order. The hands of the ranges and the attempts reference them:
-- a hand cannot have an action that does not exist in its situation.
CREATE TABLE app.situation_action (
    situation text     NOT NULL REFERENCES app.situation,
    seq       smallint NOT NULL CHECK (seq >= 1),
    action    text     NOT NULL CHECK (action IN ('MR_4B_C', 'MR_C_C', 'MR_C_F', 'MR_F_F', 'L_C_C', 'L_C_F', 'L_PUSH', 'L_F',
                                                  'ALLIN', '3BET', '3BET_C', 'CALL', 'CALL_VS_X2', 'ISO_C', 'ISO_F',
                                                  'LIMP', 'CHECK', 'FOLD')),
    PRIMARY KEY (situation, action),
    UNIQUE (situation, seq)
);

GRANT SELECT ON app.hand, app.situation, app.situation_prior_action, app.situation_stack, app.situation_action
    TO ${app_role};
