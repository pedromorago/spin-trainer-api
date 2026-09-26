-- Highest version each user's custom range of a spot has ever had. It survives deleting the range, so one created
-- again continues the count instead of starting over at 1: a stale `version` can never match a different range, and
-- (rangeSource, rangeVersion) on an attempt keeps identifying the contents it was graded against (ADR-0007, ADR-0013).
CREATE TABLE app.user_range_version (
    user_id      uuid          NOT NULL,
    situation    text          NOT NULL,
    stack        numeric(4, 1) NOT NULL,
    last_version integer       NOT NULL CHECK (last_version >= 1),
    PRIMARY KEY (user_id, situation, stack),
    FOREIGN KEY (situation, stack) REFERENCES app.situation_stack
);

INSERT INTO app.user_range_version (user_id, situation, stack, last_version)
SELECT user_id, situation, stack, version FROM app.user_range;

-- Only moves forward: the API never deletes a row.
GRANT SELECT, INSERT, UPDATE ON app.user_range_version TO ${app_role};
