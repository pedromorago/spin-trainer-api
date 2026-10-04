-- The situation bb_vs_sb_os: the BB facing the SB's open-shove after the BTN folds, the 3-max counterpart of
-- hu_bb_vs_os, with one range per stack from 20 down to 4 BB. This migration also seeded its first reference ranges,
-- from a third party's table, since taken out of the repository (ADR-0024): its reference ranges are V9's examples.

-- Right after bb_vs_sb_limp, with the rest of the BB vs SB spots (positions are unique: shifted in two steps).
UPDATE app.situation SET position = position + 100 WHERE position >= 7;
UPDATE app.situation SET position = position - 99 WHERE position >= 107;

INSERT INTO app.situation (key, position, label, format, hero, notes) VALUES
    ('bb_vs_sb_os', 7, 'BB vs SB Open-Shove', '3max', 'BB', NULL);

INSERT INTO app.situation_prior_action (situation, seq, position, action) VALUES
    ('bb_vs_sb_os', 1, 'BTN', 'FOLD'),
    ('bb_vs_sb_os', 2, 'SB', 'SHOVE');

INSERT INTO app.situation_stack (situation, stack) VALUES
    ('bb_vs_sb_os', 20),
    ('bb_vs_sb_os', 15),
    ('bb_vs_sb_os', 12),
    ('bb_vs_sb_os', 10),
    ('bb_vs_sb_os', 8),
    ('bb_vs_sb_os', 6),
    ('bb_vs_sb_os', 4);

INSERT INTO app.situation_action (situation, seq, action) VALUES
    ('bb_vs_sb_os', 1, 'CALL'),
    ('bb_vs_sb_os', 2, 'FOLD');
