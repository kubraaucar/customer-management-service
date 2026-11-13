-- Example customers for the public demo, so a visitor does not start with an empty list.
-- Loaded only when this folder is added to spring.flyway.locations (see render.yaml);
-- local development and tests do not use it.
--
-- The people are made up. The TC numbers deliberately fail the TC checksum,
-- so they can never be the number of a real person.
-- Existing numbers are skipped, so customers the administrator deleted or changed stay that way.
INSERT INTO customers (id, name, surname, citizen_number, birth_date, is_active) VALUES
    (gen_random_uuid(), 'Ali',     'Yılmaz',   10000000001, DATE '1990-04-12', TRUE),
    (gen_random_uuid(), 'Ayşe',    'Demir',    10000000002, DATE '1985-09-30', TRUE),
    (gen_random_uuid(), 'Mehmet',  'Kaya',     10000000003, DATE '1978-01-05', FALSE),
    (gen_random_uuid(), 'Zeynep',  'Şahin',    10000000004, DATE '2001-07-19', TRUE),
    (gen_random_uuid(), 'Mustafa', 'Çelik',    10000000005, NULL,              TRUE),
    (gen_random_uuid(), 'Elif',    'Öztürk',   10000000006, DATE '1995-12-02', TRUE),
    (gen_random_uuid(), 'Ahmet',   'Aydın',    10000000007, DATE '1969-03-23', FALSE),
    (gen_random_uuid(), 'Fatma',   'Arslan',   10000000008, DATE '1988-11-11', TRUE),
    (gen_random_uuid(), 'Emre',    'Doğan',    10000000009, DATE '1999-02-14', TRUE),
    (gen_random_uuid(), 'Selin',   'Koç',      10000000010, DATE '2003-05-08', TRUE),
    (gen_random_uuid(), 'Burak',   'Kurt',     10000000011, DATE '1992-08-27', FALSE),
    (gen_random_uuid(), 'İrem',    'Özdemir',  10000000012, DATE '1997-10-10', TRUE),
    (gen_random_uuid(), 'Can',     'Polat',    10000000013, DATE '1983-06-17', TRUE),
    (gen_random_uuid(), 'Deniz',   'Erdoğan',  10000000014, DATE '2000-01-29', TRUE),
    (gen_random_uuid(), 'Gizem',   'Yıldız',   10000000015, DATE '1994-04-03', FALSE)
ON CONFLICT (citizen_number) DO NOTHING;
