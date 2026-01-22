ALTER TABLE IF EXISTS book ALTER COLUMN available_copies TYPE integer USING available_copies::integer;

ALTER TABLE IF EXISTS book ALTER COLUMN total_copies TYPE integer USING total_copies::integer;