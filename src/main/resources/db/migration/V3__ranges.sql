-- Reference ranges (seed from the PDF, ADR-0006) and custom ranges of the user (ADR-0012). One row per hand with an
-- explicit action; missing ones have the implicit action of the situation. `version`: the seed's, or the optimistic
-- concurrency one for custom ranges (ADR-0013).

CREATE TABLE app.default_range (
    situation text          NOT NULL,
    stack     numeric(4, 1) NOT NULL,
    version   integer       NOT NULL CHECK (version >= 1),
    PRIMARY KEY (situation, stack),
    FOREIGN KEY (situation, stack) REFERENCES app.situation_stack
);

CREATE TABLE app.default_range_hand (
    situation text          NOT NULL,
    stack     numeric(4, 1) NOT NULL,
    hand      text          NOT NULL REFERENCES app.hand,
    action    text          NOT NULL,
    PRIMARY KEY (situation, stack, hand),
    FOREIGN KEY (situation, stack) REFERENCES app.default_range ON DELETE CASCADE,
    FOREIGN KEY (situation, action) REFERENCES app.situation_action (situation, action)
);

-- user_id is the Supabase JWT `sub`; no foreign key to auth.users (the API does not couple to the Supabase schema).
CREATE TABLE app.user_range (
    user_id    uuid          NOT NULL,
    situation  text          NOT NULL,
    stack      numeric(4, 1) NOT NULL,
    version    integer       NOT NULL CHECK (version >= 1),
    updated_at timestamptz   NOT NULL,
    PRIMARY KEY (user_id, situation, stack),
    FOREIGN KEY (situation, stack) REFERENCES app.situation_stack
);

CREATE TABLE app.user_range_hand (
    user_id   uuid          NOT NULL,
    situation text          NOT NULL,
    stack     numeric(4, 1) NOT NULL,
    hand      text          NOT NULL REFERENCES app.hand,
    action    text          NOT NULL,
    PRIMARY KEY (user_id, situation, stack, hand),
    FOREIGN KEY (user_id, situation, stack) REFERENCES app.user_range ON DELETE CASCADE,
    FOREIGN KEY (situation, action) REFERENCES app.situation_action (situation, action)
);

-- The API only reads the reference ones (written by the migrations) and manages the user's ones.
GRANT SELECT ON app.default_range, app.default_range_hand TO ${app_role};
GRANT SELECT, INSERT, UPDATE, DELETE ON app.user_range, app.user_range_hand TO ${app_role};
