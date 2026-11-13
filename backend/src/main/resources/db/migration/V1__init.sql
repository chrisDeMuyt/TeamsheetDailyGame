-- Teamsheet v1 schema (PostgreSQL 15+)
-- Pool per match: starting XI + subs who came on. 2000 onward.

CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- fuzzy name matching
CREATE EXTENSION IF NOT EXISTS unaccent;  -- "Šmicer" == "smicer"

-- ---------- Content ----------

CREATE TABLE player (
    id              BIGSERIAL PRIMARY KEY,
    display_name    TEXT NOT NULL,                 -- "Vladimír Šmicer"
    short_name      TEXT NOT NULL,                 -- "Šmicer" (pitch label)
    seed_rarity     NUMERIC(5,2)                   -- 0-100 proxy (pageviews/caps) for cold start
);

CREATE TABLE player_alias (
    player_id       BIGINT NOT NULL REFERENCES player(id) ON DELETE CASCADE,
    alias_norm      TEXT NOT NULL,                 -- normalized: lower(unaccent(...)), e.g. "smicer", "xabi"
    PRIMARY KEY (player_id, alias_norm)
);
CREATE INDEX player_alias_trgm ON player_alias USING gin (alias_norm gin_trgm_ops);

CREATE TYPE verification AS ENUM ('verified', 'single_source', 'unverified');

CREATE TABLE match (
    id              TEXT PRIMARY KEY,              -- "2005-ucl-final-liverpool"
    match_date      DATE NOT NULL CHECK (match_date >= DATE '2000-01-01'),
    competition     TEXT NOT NULL,
    team            TEXT NOT NULL,                 -- the side being guessed
    opponent        TEXT NOT NULL,
    result          TEXT NOT NULL,
    formation       TEXT NOT NULL,                 -- "4-4-1-1"
    xi_status       verification NOT NULL,
    subs_status     verification NOT NULL,
    sources         TEXT[] NOT NULL
);

CREATE TYPE squad_role AS ENUM ('starter', 'sub_used');

CREATE TABLE match_player (
    match_id        TEXT   NOT NULL REFERENCES match(id) ON DELETE CASCADE,
    player_id       BIGINT NOT NULL REFERENCES player(id),
    role            squad_role NOT NULL,
    slot_pos        TEXT NOT NULL,                 -- "LB", "CAM", "MF" (subs)
    slot_x          SMALLINT,                      -- pitch coords 0-100 for starters; NULL for subs
    slot_y          SMALLINT,
    minute_on       SMALLINT,                      -- subs only
    minute_off      SMALLINT,
    replaced_id     BIGINT REFERENCES player(id),  -- sub_used: who they came on for
    PRIMARY KEY (match_id, player_id)
);

-- ---------- Daily puzzles ----------

CREATE TABLE puzzle (
    number          INT PRIMARY KEY,               -- Teamsheet #142
    puzzle_date     DATE NOT NULL UNIQUE,
    match_id        TEXT NOT NULL REFERENCES match(id)
);

-- ---------- Plays (no accounts; anon id from localStorage) ----------

CREATE TABLE play (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    puzzle_number   INT  NOT NULL REFERENCES puzzle(number),
    anon_id         UUID NOT NULL,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    guess_opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),   -- server-side 25s timer
    finished_at     TIMESTAMPTZ,
    total_score     SMALLINT NOT NULL DEFAULT 0,
    UNIQUE (puzzle_number, anon_id)                       -- one play per device per day
);

CREATE TABLE guess (
    play_id         UUID NOT NULL REFERENCES play(id) ON DELETE CASCADE,
    guess_index     SMALLINT NOT NULL CHECK (guess_index BETWEEN 1 AND 3),
    raw_text        TEXT NOT NULL,
    player_id       BIGINT REFERENCES player(id),          -- NULL = miss / timeout
    tier            TEXT,                                  -- "Cult Hero", ...
    points          SMALLINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (play_id, guess_index)
);
-- duplicate block: a player can only score once per play
CREATE UNIQUE INDEX guess_no_dupes ON guess (play_id, player_id) WHERE player_id IS NOT NULL;

-- ---------- Rarity counters (bumped on each hit; read at reveal) ----------

CREATE TABLE pick_stat (
    puzzle_number   INT    NOT NULL REFERENCES puzzle(number),
    player_id       BIGINT NOT NULL REFERENCES player(id),
    picks           INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (puzzle_number, player_id)
);

-- pick % = picks / finished plays for that puzzle
CREATE VIEW pick_pct AS
SELECT ps.puzzle_number,
       ps.player_id,
       ps.picks,
       ROUND(100.0 * ps.picks / NULLIF(p.plays, 0), 1) AS pct
FROM pick_stat ps
JOIN (SELECT puzzle_number, COUNT(*) AS plays
      FROM play WHERE finished_at IS NOT NULL
      GROUP BY puzzle_number) p USING (puzzle_number);
