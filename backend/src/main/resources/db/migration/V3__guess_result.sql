-- Distinguish timeouts from misses (both have player_id NULL) so history/reveal can say "Time's up".
ALTER TABLE guess ADD COLUMN result TEXT;
UPDATE guess SET result = CASE WHEN player_id IS NULL THEN 'miss' ELSE 'hit' END;
ALTER TABLE guess ALTER COLUMN result SET NOT NULL;
ALTER TABLE guess ADD CONSTRAINT guess_result_chk CHECK (result IN ('hit', 'miss', 'timeout'));
