-- Rangos de referencia (seed del PDF, ADR-0006) y personalizados del usuario (ADR-0012). Una fila por mano con acción
-- explícita; las que faltan tienen la implícita de la situación. `version`: la del seed o la de la concurrencia
-- optimista de los personalizados (ADR-0013).

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

-- user_id es el `sub` del JWT de Supabase; sin clave foránea a auth.users (la API no se acopla al esquema de Supabase).
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

-- La API solo lee los de referencia (los escriben las migraciones) y gestiona los del usuario.
GRANT SELECT ON app.default_range, app.default_range_hand TO ${app_role};
GRANT SELECT, INSERT, UPDATE, DELETE ON app.user_range, app.user_range_hand TO ${app_role};
