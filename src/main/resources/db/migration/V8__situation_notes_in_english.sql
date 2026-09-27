-- The app is in English (ADR-0021): the situation notes seeded by V2, V5 and V7 in Spanish, translated. The notes are
-- the PDF's own advice, so the meaning is kept word for word; L/C/F, MR/F/F and "3H OS call" are the PDF's labels.

UPDATE app.situation SET notes = 'At 25 BB, against a 3-bet to 3 BB, yellow (MR/F/F) is a call.'
WHERE key = 'btn_open';

UPDATE app.situation SET notes = 'The suited part of yellow can be played L/C/F. Use the gray part only against a passive fish.'
WHERE key = 'sb_open';

UPDATE app.situation SET notes = 'Call the green hands only against two fish.'
WHERE key = 'sb_vs_btn_mr';

UPDATE app.situation SET notes = 'The PDF''s "3H OS call" chart: each hand calls the shove when the effective stack is at most its threshold in BB.'
WHERE key = 'bb_vs_sb_os';
