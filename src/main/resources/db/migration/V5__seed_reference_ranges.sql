-- Corrections to the V2 catalog, written before the situations' legends were known: sb_open has L/F (limp/fold)
-- instead of L/C/C, and hu_sb_open has an open-shove. This migration also seeded the first reference ranges, a third
-- party's tables, since taken out of the repository (ADR-0024): the reference ranges are V9's examples.

-- sb_open: L/F instead of L/C/C.
DELETE FROM app.situation_action WHERE situation = 'sb_open' AND action = 'L_C_C';
UPDATE app.situation_action SET seq = 5 WHERE situation = 'sb_open' AND action = 'L_C_F';
INSERT INTO app.situation_action (situation, seq, action) VALUES ('sb_open', 6, 'L_F');

-- hu_sb_open: open-shove.
UPDATE app.situation_action SET seq = 10 WHERE situation = 'hu_sb_open' AND action = 'FOLD';
INSERT INTO app.situation_action (situation, seq, action) VALUES ('hu_sb_open', 9, 'ALLIN');
