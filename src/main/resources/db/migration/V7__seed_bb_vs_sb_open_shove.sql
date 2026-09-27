-- The "3H OS call" table of Tablasmentov3.pdf (page 19), left out of V2 and V5: it is not a coloured range but one
-- threshold per hand (call an open-shove when the effective stack is at most that many BB). It becomes the situation
-- bb_vs_sb_os, the 3-max counterpart of hu_bb_vs_os ("HU vs OS", page 20): the BB facing the SB's open-shove after the
-- BTN folds. Its reference ranges are derived per stack from the thresholds (reference-os-call-thresholds.json):
-- CALL if stack <= threshold. Same content as reference-ranges.json (ReferenceSeedIT).

-- Right after bb_vs_sb_limp, with the rest of the BB vs SB spots (positions are unique: shifted in two steps).
UPDATE app.situation SET position = position + 100 WHERE position >= 7;
UPDATE app.situation SET position = position - 99 WHERE position >= 107;

INSERT INTO app.situation (key, position, label, format, hero, notes) VALUES
    ('bb_vs_sb_os', 7, 'BB vs SB Open-Shove', '3max', 'BB',
     'Tabla «3H OS call» del PDF: cada mano paga el shove si el stack efectivo es igual o menor que su umbral en BB.');

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

WITH pdf (situation, stack, hands) AS (VALUES
    ('bb_vs_sb_os', 20, '{
        "AA": "CALL", "AKs": "CALL", "AQs": "CALL", "AJs": "CALL", "ATs": "CALL", "A9s": "CALL", "A8s": "CALL", "A7s": "CALL", "A6s": "CALL", "A5s": "CALL", "A4s": "CALL", "A3s": "CALL", "A2s": "CALL",
        "AKo": "CALL", "KK": "CALL", "KQs": "CALL", "KJs": "CALL", "KTs": "CALL", "K9s": "CALL",
        "AQo": "CALL", "KQo": "CALL", "QQ": "CALL", "QJs": "CALL", "QTs": "CALL",
        "AJo": "CALL", "KJo": "CALL", "JJ": "CALL",
        "ATo": "CALL", "KTo": "CALL", "TT": "CALL",
        "A9o": "CALL", "99": "CALL",
        "A8o": "CALL", "88": "CALL",
        "A7o": "CALL", "77": "CALL",
        "A6o": "CALL", "66": "CALL",
        "A5o": "CALL", "55": "CALL",
        "44": "CALL",
        "33": "CALL"
    }'::jsonb),
    ('bb_vs_sb_os', 15, '{
        "AA": "CALL", "AKs": "CALL", "AQs": "CALL", "AJs": "CALL", "ATs": "CALL", "A9s": "CALL", "A8s": "CALL", "A7s": "CALL", "A6s": "CALL", "A5s": "CALL", "A4s": "CALL", "A3s": "CALL", "A2s": "CALL",
        "AKo": "CALL", "KK": "CALL", "KQs": "CALL", "KJs": "CALL", "KTs": "CALL", "K9s": "CALL", "K8s": "CALL", "K7s": "CALL",
        "AQo": "CALL", "KQo": "CALL", "QQ": "CALL", "QJs": "CALL", "QTs": "CALL", "Q9s": "CALL",
        "AJo": "CALL", "KJo": "CALL", "QJo": "CALL", "JJ": "CALL", "JTs": "CALL",
        "ATo": "CALL", "KTo": "CALL", "QTo": "CALL", "TT": "CALL",
        "A9o": "CALL", "K9o": "CALL", "99": "CALL",
        "A8o": "CALL", "88": "CALL",
        "A7o": "CALL", "77": "CALL",
        "A6o": "CALL", "66": "CALL",
        "A5o": "CALL", "55": "CALL",
        "A4o": "CALL", "44": "CALL",
        "A3o": "CALL", "33": "CALL",
        "A2o": "CALL", "22": "CALL"
    }'::jsonb),
    ('bb_vs_sb_os', 12, '{
        "AA": "CALL", "AKs": "CALL", "AQs": "CALL", "AJs": "CALL", "ATs": "CALL", "A9s": "CALL", "A8s": "CALL", "A7s": "CALL", "A6s": "CALL", "A5s": "CALL", "A4s": "CALL", "A3s": "CALL", "A2s": "CALL",
        "AKo": "CALL", "KK": "CALL", "KQs": "CALL", "KJs": "CALL", "KTs": "CALL", "K9s": "CALL", "K8s": "CALL", "K7s": "CALL", "K6s": "CALL", "K5s": "CALL", "K4s": "CALL",
        "AQo": "CALL", "KQo": "CALL", "QQ": "CALL", "QJs": "CALL", "QTs": "CALL", "Q9s": "CALL", "Q8s": "CALL",
        "AJo": "CALL", "KJo": "CALL", "QJo": "CALL", "JJ": "CALL", "JTs": "CALL", "J9s": "CALL",
        "ATo": "CALL", "KTo": "CALL", "QTo": "CALL", "JTo": "CALL", "TT": "CALL",
        "A9o": "CALL", "K9o": "CALL", "99": "CALL",
        "A8o": "CALL", "K8o": "CALL", "88": "CALL",
        "A7o": "CALL", "K7o": "CALL", "77": "CALL",
        "A6o": "CALL", "66": "CALL",
        "A5o": "CALL", "55": "CALL",
        "A4o": "CALL", "44": "CALL",
        "A3o": "CALL", "33": "CALL",
        "A2o": "CALL", "22": "CALL"
    }'::jsonb),
    ('bb_vs_sb_os', 10, '{
        "AA": "CALL", "AKs": "CALL", "AQs": "CALL", "AJs": "CALL", "ATs": "CALL", "A9s": "CALL", "A8s": "CALL", "A7s": "CALL", "A6s": "CALL", "A5s": "CALL", "A4s": "CALL", "A3s": "CALL", "A2s": "CALL",
        "AKo": "CALL", "KK": "CALL", "KQs": "CALL", "KJs": "CALL", "KTs": "CALL", "K9s": "CALL", "K8s": "CALL", "K7s": "CALL", "K6s": "CALL", "K5s": "CALL", "K4s": "CALL", "K3s": "CALL", "K2s": "CALL",
        "AQo": "CALL", "KQo": "CALL", "QQ": "CALL", "QJs": "CALL", "QTs": "CALL", "Q9s": "CALL", "Q8s": "CALL", "Q7s": "CALL", "Q6s": "CALL",
        "AJo": "CALL", "KJo": "CALL", "QJo": "CALL", "JJ": "CALL", "JTs": "CALL", "J9s": "CALL", "J8s": "CALL",
        "ATo": "CALL", "KTo": "CALL", "QTo": "CALL", "JTo": "CALL", "TT": "CALL", "T9s": "CALL",
        "A9o": "CALL", "K9o": "CALL", "Q9o": "CALL", "99": "CALL",
        "A8o": "CALL", "K8o": "CALL", "88": "CALL",
        "A7o": "CALL", "K7o": "CALL", "77": "CALL",
        "A6o": "CALL", "K6o": "CALL", "66": "CALL",
        "A5o": "CALL", "K5o": "CALL", "55": "CALL",
        "A4o": "CALL", "44": "CALL",
        "A3o": "CALL", "33": "CALL",
        "A2o": "CALL", "22": "CALL"
    }'::jsonb),
    ('bb_vs_sb_os', 8, '{
        "AA": "CALL", "AKs": "CALL", "AQs": "CALL", "AJs": "CALL", "ATs": "CALL", "A9s": "CALL", "A8s": "CALL", "A7s": "CALL", "A6s": "CALL", "A5s": "CALL", "A4s": "CALL", "A3s": "CALL", "A2s": "CALL",
        "AKo": "CALL", "KK": "CALL", "KQs": "CALL", "KJs": "CALL", "KTs": "CALL", "K9s": "CALL", "K8s": "CALL", "K7s": "CALL", "K6s": "CALL", "K5s": "CALL", "K4s": "CALL", "K3s": "CALL", "K2s": "CALL",
        "AQo": "CALL", "KQo": "CALL", "QQ": "CALL", "QJs": "CALL", "QTs": "CALL", "Q9s": "CALL", "Q8s": "CALL", "Q7s": "CALL", "Q6s": "CALL", "Q5s": "CALL", "Q4s": "CALL",
        "AJo": "CALL", "KJo": "CALL", "QJo": "CALL", "JJ": "CALL", "JTs": "CALL", "J9s": "CALL", "J8s": "CALL", "J7s": "CALL",
        "ATo": "CALL", "KTo": "CALL", "QTo": "CALL", "JTo": "CALL", "TT": "CALL", "T9s": "CALL", "T8s": "CALL",
        "A9o": "CALL", "K9o": "CALL", "Q9o": "CALL", "J9o": "CALL", "T9o": "CALL", "99": "CALL", "98s": "CALL",
        "A8o": "CALL", "K8o": "CALL", "Q8o": "CALL", "88": "CALL",
        "A7o": "CALL", "K7o": "CALL", "Q7o": "CALL", "77": "CALL",
        "A6o": "CALL", "K6o": "CALL", "66": "CALL",
        "A5o": "CALL", "K5o": "CALL", "55": "CALL",
        "A4o": "CALL", "K4o": "CALL", "44": "CALL",
        "A3o": "CALL", "K3o": "CALL", "33": "CALL",
        "A2o": "CALL", "K2o": "CALL", "22": "CALL"
    }'::jsonb),
    ('bb_vs_sb_os', 6, '{
        "AA": "CALL", "AKs": "CALL", "AQs": "CALL", "AJs": "CALL", "ATs": "CALL", "A9s": "CALL", "A8s": "CALL", "A7s": "CALL", "A6s": "CALL", "A5s": "CALL", "A4s": "CALL", "A3s": "CALL", "A2s": "CALL",
        "AKo": "CALL", "KK": "CALL", "KQs": "CALL", "KJs": "CALL", "KTs": "CALL", "K9s": "CALL", "K8s": "CALL", "K7s": "CALL", "K6s": "CALL", "K5s": "CALL", "K4s": "CALL", "K3s": "CALL", "K2s": "CALL",
        "AQo": "CALL", "KQo": "CALL", "QQ": "CALL", "QJs": "CALL", "QTs": "CALL", "Q9s": "CALL", "Q8s": "CALL", "Q7s": "CALL", "Q6s": "CALL", "Q5s": "CALL", "Q4s": "CALL", "Q3s": "CALL", "Q2s": "CALL",
        "AJo": "CALL", "KJo": "CALL", "QJo": "CALL", "JJ": "CALL", "JTs": "CALL", "J9s": "CALL", "J8s": "CALL", "J7s": "CALL", "J6s": "CALL", "J5s": "CALL", "J4s": "CALL",
        "ATo": "CALL", "KTo": "CALL", "QTo": "CALL", "JTo": "CALL", "TT": "CALL", "T9s": "CALL", "T8s": "CALL", "T7s": "CALL", "T6s": "CALL",
        "A9o": "CALL", "K9o": "CALL", "Q9o": "CALL", "J9o": "CALL", "T9o": "CALL", "99": "CALL", "98s": "CALL", "97s": "CALL",
        "A8o": "CALL", "K8o": "CALL", "Q8o": "CALL", "J8o": "CALL", "T8o": "CALL", "98o": "CALL", "88": "CALL", "87s": "CALL",
        "A7o": "CALL", "K7o": "CALL", "Q7o": "CALL", "J7o": "CALL", "77": "CALL",
        "A6o": "CALL", "K6o": "CALL", "Q6o": "CALL", "66": "CALL",
        "A5o": "CALL", "K5o": "CALL", "Q5o": "CALL", "55": "CALL",
        "A4o": "CALL", "K4o": "CALL", "Q4o": "CALL", "44": "CALL",
        "A3o": "CALL", "K3o": "CALL", "33": "CALL",
        "A2o": "CALL", "K2o": "CALL", "22": "CALL"
    }'::jsonb),
    ('bb_vs_sb_os', 4, '{
        "AA": "CALL", "AKs": "CALL", "AQs": "CALL", "AJs": "CALL", "ATs": "CALL", "A9s": "CALL", "A8s": "CALL", "A7s": "CALL", "A6s": "CALL", "A5s": "CALL", "A4s": "CALL", "A3s": "CALL", "A2s": "CALL",
        "AKo": "CALL", "KK": "CALL", "KQs": "CALL", "KJs": "CALL", "KTs": "CALL", "K9s": "CALL", "K8s": "CALL", "K7s": "CALL", "K6s": "CALL", "K5s": "CALL", "K4s": "CALL", "K3s": "CALL", "K2s": "CALL",
        "AQo": "CALL", "KQo": "CALL", "QQ": "CALL", "QJs": "CALL", "QTs": "CALL", "Q9s": "CALL", "Q8s": "CALL", "Q7s": "CALL", "Q6s": "CALL", "Q5s": "CALL", "Q4s": "CALL", "Q3s": "CALL", "Q2s": "CALL",
        "AJo": "CALL", "KJo": "CALL", "QJo": "CALL", "JJ": "CALL", "JTs": "CALL", "J9s": "CALL", "J8s": "CALL", "J7s": "CALL", "J6s": "CALL", "J5s": "CALL", "J4s": "CALL", "J3s": "CALL", "J2s": "CALL",
        "ATo": "CALL", "KTo": "CALL", "QTo": "CALL", "JTo": "CALL", "TT": "CALL", "T9s": "CALL", "T8s": "CALL", "T7s": "CALL", "T6s": "CALL", "T5s": "CALL", "T4s": "CALL", "T3s": "CALL", "T2s": "CALL",
        "A9o": "CALL", "K9o": "CALL", "Q9o": "CALL", "J9o": "CALL", "T9o": "CALL", "99": "CALL", "98s": "CALL", "97s": "CALL", "96s": "CALL", "95s": "CALL", "94s": "CALL", "93s": "CALL",
        "A8o": "CALL", "K8o": "CALL", "Q8o": "CALL", "J8o": "CALL", "T8o": "CALL", "98o": "CALL", "88": "CALL", "87s": "CALL", "86s": "CALL", "85s": "CALL", "84s": "CALL",
        "A7o": "CALL", "K7o": "CALL", "Q7o": "CALL", "J7o": "CALL", "T7o": "CALL", "97o": "CALL", "87o": "CALL", "77": "CALL", "76s": "CALL", "75s": "CALL", "74s": "CALL",
        "A6o": "CALL", "K6o": "CALL", "Q6o": "CALL", "J6o": "CALL", "T6o": "CALL", "96o": "CALL", "86o": "CALL", "76o": "CALL", "66": "CALL", "65s": "CALL", "64s": "CALL",
        "A5o": "CALL", "K5o": "CALL", "Q5o": "CALL", "J5o": "CALL", "T5o": "CALL", "55": "CALL", "54s": "CALL", "53s": "CALL",
        "A4o": "CALL", "K4o": "CALL", "Q4o": "CALL", "J4o": "CALL", "44": "CALL",
        "A3o": "CALL", "K3o": "CALL", "Q3o": "CALL", "J3o": "CALL", "33": "CALL",
        "A2o": "CALL", "K2o": "CALL", "Q2o": "CALL", "J2o": "CALL", "22": "CALL"
    }'::jsonb)
),
reference AS (
    INSERT INTO app.default_range (situation, stack, version)
    SELECT situation, stack, 1 FROM pdf
)
INSERT INTO app.default_range_hand (situation, stack, hand, action)
SELECT pdf.situation, pdf.stack, h.key, h.value
FROM pdf, jsonb_each_text(pdf.hands) AS h;
