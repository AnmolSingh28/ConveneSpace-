INSERT INTO event_categories (id, name, active)
VALUES
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567801', 'Live Music', true),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567802', 'Comedy Shows', true),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567803', 'Stand-up Specials', true),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567804', 'Theatre & Drama', true),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567805', 'Food & Drink Festivals', true),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567806', 'Art Exhibitions', true),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567807', 'Sports Events', true),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567808', 'Workshops & Masterclasses', true),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567809', 'Film Screenings', true),
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567810', 'Cultural Festivals', true)
ON CONFLICT DO NOTHING;