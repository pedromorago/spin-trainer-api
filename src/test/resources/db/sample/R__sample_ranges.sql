-- Solo desarrollo (bootTestRun): el rango de ejemplo del mock de la web, para que la API local se comporte igual.
-- Los rangos reales del PDF llegan como migraciones versionadas en db/migration, validados con Pedro (roadmap 2).

INSERT INTO app.default_range (situation, stack, version) VALUES ('btn_open', 25, 1) ON CONFLICT DO NOTHING;

INSERT INTO app.default_range_hand (situation, stack, hand, action) VALUES
    ('btn_open', 25, '22', 'L_C_C'),
    ('btn_open', 25, '33', 'L_C_C'),
    ('btn_open', 25, '44', 'L_C_C'),
    ('btn_open', 25, '55', 'L_C_C'),
    ('btn_open', 25, '66', 'MR_F_F'),
    ('btn_open', 25, '77', 'MR_F_F'),
    ('btn_open', 25, '88', 'MR_C_F'),
    ('btn_open', 25, '99', 'MR_C_F'),
    ('btn_open', 25, 'AA', 'MR_4B_C'),
    ('btn_open', 25, 'KK', 'MR_4B_C'),
    ('btn_open', 25, 'QQ', 'MR_4B_C'),
    ('btn_open', 25, 'AKs', 'MR_4B_C'),
    ('btn_open', 25, 'AKo', 'MR_4B_C'),
    ('btn_open', 25, 'JJ', 'MR_C_C'),
    ('btn_open', 25, 'TT', 'MR_C_C'),
    ('btn_open', 25, 'AQs', 'MR_C_C'),
    ('btn_open', 25, 'AQo', 'MR_C_C'),
    ('btn_open', 25, 'AJs', 'MR_C_F'),
    ('btn_open', 25, 'KQs', 'MR_C_F'),
    ('btn_open', 25, 'ATs', 'MR_F_F'),
    ('btn_open', 25, 'KJs', 'MR_F_F'),
    ('btn_open', 25, 'QJs', 'MR_F_F'),
    ('btn_open', 25, 'AJo', 'MR_F_F'),
    ('btn_open', 25, 'KQo', 'MR_F_F'),
    ('btn_open', 25, 'T9s', 'L_C_F'),
    ('btn_open', 25, '98s', 'L_C_F'),
    ('btn_open', 25, '87s', 'L_C_F'),
    ('btn_open', 25, '76s', 'L_C_F')
ON CONFLICT DO NOTHING;
