-- Catálogo: manos del grid 13×13 y las situaciones de Spin & Go (ADR-0011) con sus stacks, acciones y acciones previas.
-- Flyway se ejecuta como spin_migrator, dueño del esquema `app`; la API se conecta como ${app_role} (ADR-0015).

REVOKE ALL ON SCHEMA app FROM PUBLIC;
GRANT USAGE ON SCHEMA app TO ${app_role};

-- Las 169 manos canónicas: pareja (AA), alta + baja + s/o (AKs, T9o). Las tablas de manos la referencian,
-- así la base de datos rechaza "AAs", "AK" o "KAs" igual que el dominio.
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

-- Lo que hicieron los jugadores anteriores, en orden; vacío si abre el héroe.
CREATE TABLE app.situation_prior_action (
    situation text     NOT NULL REFERENCES app.situation,
    seq       smallint NOT NULL CHECK (seq >= 1),
    position  text     NOT NULL CHECK (position IN ('BTN', 'SB', 'BB')),
    action    text     NOT NULL CHECK (action IN ('FOLD', 'LIMP', 'MIN_RAISE', 'RAISE', 'THREE_BET', 'CALL', 'SHOVE')),
    PRIMARY KEY (situation, seq)
);

-- Stacks efectivos en BB, múltiplos de 0,5 (12.5 existe).
CREATE TABLE app.situation_stack (
    situation text         NOT NULL REFERENCES app.situation,
    stack     numeric(4, 1) NOT NULL CHECK (stack BETWEEN 1 AND 100 AND stack * 2 = trunc(stack * 2)),
    PRIMARY KEY (situation, stack)
);

-- Acciones posibles de cada situación, en el orden de la paleta. Las manos de los rangos y los intentos las
-- referencian: una mano no puede tener una acción que no existe en su situación.
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
