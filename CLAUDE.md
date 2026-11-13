# Teamsheet

Daily football trivia game. Each day shows one historic match lineup (positions only); name 3 players from it.
Rarer picks score more. Live at **https://teamsheet-daily.fly.dev** · repo `chrisDeMuyt/TeamsheetDailyGame` (`main`).

## Current state (2026-11-13)

- Friends beta running (≈10 devices). Puzzles #1–54 live, one per day, **2026-11-11 → 2027-01-03** (add more before then).
- 6 medium-confidence drafts left in `teamsheet-drafts.json` (open questions in each match's `flags`).
- ~5,000 extra players (Wikidata) back the "Did you mean" suggestions.
- Tests: backend 53 (JUnit + Testcontainers), frontend 31 (Vitest). Both must pass before a deploy.

## Statistics / analytics (how to check how the game is doing)

```bash
bash deploy/beta-stats.sh          # from the repo root; in a Claude Code chat: ! bash deploy/beta-stats.sh
```
- Needs flyctl logged in (`~/.fly/bin/flyctl.exe auth login` if it says no access token). Takes ~3s.
- Read-only: `deploy/beta-stats.sql` runs inside `BEGIN TRANSACTION READ ONLY` over `fly postgres connect`
  (app `teamsheet-daily-db`, database `teamsheet_daily`). It cannot change data; temp views are refused too.
- Excludes test devices listed in `\set test_devices` at the top of the SQL (smoke-test anonId `00000000-0000-4000-8000-0000000000aa`).
- Sections: Overview (devices, plays, finished, finish %, avg score) · By puzzle (newest first) · Returning devices
  (days played per device) · Guess outcomes (hit/miss/timeout %) · Score spread · Most picked players (last 7 puzzles,
  top 3) · Most common wrong guesses (spot missing aliases / confusing lineups).
- "Devices" = anonIds in localStorage (no accounts): one person on phone + laptop counts twice.
- To add a stat: add a `\echo` heading + one single-line `SELECT ... WHERE p.anon_id NOT IN (:test_devices)` query
  to `deploy/beta-stats.sql` (keep it read-only; no CREATE). The wrapper strips the terminal echo/prompts.
- Ad-hoc query: `printf "BEGIN TRANSACTION READ ONLY;\nSELECT ...;\nROLLBACK;\n\\\\q\n" | ~/.fly/bin/flyctl.exe postgres connect -a teamsheet-daily-db -d teamsheet_daily`
- Traffic/server health (requests, latency, memory): https://fly.io/apps/teamsheet-daily/monitoring
- Baseline at beta start (puzzle #2, before the Start screen): 9 devices, 78% finished, avg 55.7, 22% of guesses timed out.

## Locked decisions

| Area | Decision |
|---|---|
| Pool | Starting XI + subs who came on (no unused subs). Matches from 2000 onward |
| Picks | 3 per day, **30s each**, server-side clock (+2s network grace), duplicates blocked (don't use a guess) |
| Start | Start screen first. Today's match isn't fetched and the play/clock isn't created until Start |
| Daily reset | Midnight **US Central** (`teamsheet.game.puzzle-zone: America/Chicago`) |
| Guessing | Guesses are players. Enter submits only an **exact full name**; anything else shows one **"Did you mean: X?"** (best match across ALL players, never scoped to today) and tapping it is the guess. Unknown names/suggestions don't use a guess. A picked player whose name is an alias of exactly one lineup player counts as that player (duplicate-row backstop) |
| Scoring | Tier by pick %: 40%+ = 10 Tap-in, 15–40 = 30 Solid Shout, 5–15 = 60 Proper Fan, 1–5 = 85 Hidden Gem, <1 = 100 Cult Hero; **+15 for subs** |
| Cold start | `seed_rarity` (placeholder expected pick %) until 200 finished plays of a puzzle, then real pick % |
| UI | Card: "Team · CL Final · date" + scoreline ("Chelsea 1–1 Bayern Munich (aet, 4–3 pens)"). Pitch slots show position, then **short name** when guessed; the guess list below shows **full names** |
| Reveal | Full pool with pick % (short names), your picks highlighted, scoreline in header |
| Share | Native share sheet on phones (Web Share API: iOS + Android), clipboard on desktop. Text = header, symmetric emoji mini-pitch with picks 🟡, subs row, tier emojis (🥅 📣 🪭 💎 🐐, ❌ miss, ⏱️ timeout), streak, link. Link card image `frontend/public/og-image.png` |
| Accounts | None; `anonId` in localStorage, one play per device per day |

## Repo layout

```
backend/        Spring Boot 3.5 (Java 21, Maven wrapper), Postgres 16, Flyway migrations in src/main/resources/db/migration
  src/test/java/com/teamsheet/tools/   data tools (SeedSqlGenerator, ExtraPlayersGenerator) + their tests
frontend/       Angular 22 (standalone, signals, zoneless), Vitest. Built into the backend jar's static/ by the Dockerfile
  tools/OgImage.java                   regenerates the link-card image
data/           wikidata-footballers.json + fetch_wikidata_players.py (extra player universe)
deploy/         docker-entrypoint.sh (DATABASE_URL -> JDBC), beta-stats.sh/.sql
teamsheet-seed.json    the live matches (source of truth for migrations)
teamsheet-drafts.json  matches not yet live
docker-compose.yml     local Postgres on host port 5433
Dockerfile, fly.toml   production image + Fly config
schema.sql             original schema (= V1)
```

## Running locally (Windows machine notes)

- **JDK 21 is not on PATH**: `export JAVA_HOME="/c/Program Files/JetBrains/PyCharm 2025.2.0.1/jbr"` (IntelliJ's jbr is JDK 25). Then `backend/mvnw`.
- **Node 26** (Angular 22 needs ≥ 24.15): `C:\Program Files\nodejs`.
- **Docker Desktop** must be running (Testcontainers + local DB). `docker` lives in `C:\Program Files\Docker\Docker\resources\bin`.
- A native **PostgreSQL 18 Windows service owns port 5432**: the compose DB is on **5433**. Don't stop the user's service.
- Git Bash: use forward-slash paths (`~/.fly/bin/flyctl.exe`); backslash Windows paths break with `!` commands.
- Python with `-I` ignores `PYTHONIOENCODING`; use `python -I -X utf8 ...` when printing accented names.

```bash
docker compose up -d                                   # Postgres 16 on localhost:5433
cd backend && ./mvnw spring-boot:run                   # API on :8080, Flyway applies all migrations
cd frontend && npm start                               # ng serve on :4200, proxies /api -> :8080
cd backend && ./mvnw test                              # 53 tests, needs Docker
cd frontend && npx ng test --watch=false               # 31 tests
```
Production-like local check: `docker build -t teamsheet:local .` then
`docker run -p 8081:8080 -e DATABASE_URL='postgres://teamsheet:dev@host.docker.internal:5433/teamsheet?sslmode=disable' teamsheet:local`.

## Deploying (Fly.io)

- Apps: `teamsheet-daily` (1 shared 512 MB machine, region `ord`, stops when idle) and unmanaged Postgres
  `teamsheet-daily-db` (PG 18, **256 MB**, 1 GB volume, attached → `DATABASE_URL` secret). Org `personal`.
  Unrelated suspended app `chitown-living` exists in the org: leave it alone. Budget ≈ $3–5/month.
- Deploy from the repo root: `~/.fly/bin/flyctl.exe deploy --ha=false --remote-only` (**`--ha=false` or Fly adds a 2nd machine**).
- Free IPs only (shared v4 + v6). If a first deploy ever fails to allocate IPs: `fly ips allocate-v4 --shared` / `allocate-v6`.
- Huge migrations can crash the 256 MB Postgres (V5 did once; the app retried and recovered within a minute). Keep data migrations small; user declined upsizing.
- Verify after deploy: `curl https://teamsheet-daily.fly.dev/api/puzzle/today`; first request after idle takes ~10s (cold start).
- Analytics: see "Statistics / analytics" above (`bash deploy/beta-stats.sh`).
- No backups configured beyond Fly's volume snapshots; no CI yet.

## Data model in one paragraph

`player` (display_name, short_name, seed_rarity NULL for extras, fame = Wikipedia sitelinks) · `player_alias`
(normalized: lower, accents stripped, punctuation removed; used for suggestions) · `match` (team = side being guessed,
opponent, result from the team's side, formation, verification, sources) · `match_player` (role starter/sub_used,
slot_pos, pitch x/y for starters, minute_on/off, replaced_id) · `puzzle` (number, date, match) · `play` / `guess` /
`pick_stat` (runtime). Same person = one `player` row across all matches.

## Adding matches (the main content task)

1. **Draft** the match in `teamsheet-drafts.json` (format below). `DraftsValidationTest` checks it.
2. **Verify** XI + subs against 2 independent sources (rules below); put them in `sources`; set
   `verification.xi` / `subs_used` to `verified` (or `single_source`). Remove draft-only fields you don't need.
3. **Move** the match object into `teamsheet-seed.json`. **Position in that file = puzzle order**; new matches are
   appended after puzzle #54 (dates continue daily). Spread difficulty/clubs/eras; never put a match near its other side.
4. **Generate** the migration:
   `cd backend && ./mvnw -q test-compile exec:java -Dexec.mainClass=com.teamsheet.tools.SeedSqlGenerator -Dexec.classpathScope=test`
   → writes `Vn__puzzles_X_to_Y.sql` with only new players, `UPDATE seed_rarity` for extras now in a puzzle, aliases,
   the match, lineup, puzzles. It refuses unsourced/`unverified` matches and new players without `rarity`.
5. `./mvnw test`, commit, deploy. **Never edit an applied migration** (Flyway checksums); `SeedSqlGeneratorTest`
   fails if a shipped match's JSON changes — fix shipped data with a new hand-written migration.

### Match JSON

```json
{
  "id": "2012-ucl-final-chelsea",              // unique: year-competition-team
  "date": "2012-05-19",
  "competition": "UEFA Champions League Final", // "UEFA"/"FIFA" prefix is stripped in the UI; "Champions League" -> "CL"
  "team": "Chelsea", "opponent": "Bayern Munich",
  "result": "1-1 aet, won 4-3 on penalties",    // ALWAYS from the team's side: "lost 0-2", "2-1 aet (golden goal)", "4-0 (lost first leg 1-4)"
  "formation": "4-2-3-1",                       // must be a template in SeedSqlGenerator.FORMATIONS
  "verification": { "xi": "verified", "subs_used": "verified" },
  "sources": ["UEFA squad sheet (PDF)", "https://..."],
  "flags": ["anything a human should know"],
  "squad": [
    { "name": "Petr Čech", "pos": "GK", "role": "starter" },
    { "name": "José Bosingwa", "pos": "RB", "role": "starter" },
    { "name": "Salomon Kalou", "pos": "RW", "role": "starter", "off": 84 },
    { "name": "Fernando Torres", "pos": "FW", "role": "sub_used", "on": 84, "for": "Salomon Kalou" }
  ]
}
```
Rules the validator enforces: exactly 11 starters whose `pos` values fill the formation template exactly; repeated
positions listed **right to left** (right CB before left CB); subs use `pos` GK/DF/MF/FW and `for` someone on the
pitch whose `off` equals the sub's `on`; stoppage-time minutes are written as 90 / 120. Date ≥ 2000-01-01.

### Players

- **Name** = the player's usual full name with accents ("Djimi Traoré", "Xavi", "Hulk"). **Reuse the exact spelling**
  if he already exists (check `teamsheet-seed.json`, or the DB / `data/wikidata-footballers.json`) — that's what makes
  it the same row. Different spelling = duplicate person (the backstop helps, but don't rely on it).
- Per-player fields for players new to puzzles (on their squad entry):
  - `"rarity": 35` **required** — placeholder expected pick % (≈60 superstar, 35 well known, 18/10/4 by match
    difficulty, subs lower). Also required for an extra (Wikidata) player appearing in a puzzle for the first time.
  - `"short": "Mikel"` optional pitch label; default = last name, keeping particles (van/de/del/da/di/dos/ter/le/el →
    "van der Sar", "De Rossi", "ter Stegen"); overrides also in `SeedSqlGenerator.SHORT_NAME_OVERRIDES`.
  - `"aliases": ["cr7", "obi mikel"]` optional extra nicknames (normalized automatically). Defaults already include
    full name, short name, surname and short-name-without-spaces.
  - `"family_first": true` for East Asian names ("Park Ji-sung") so the family name ("park") is an alias.
- Adding a nickname to a player who's already live needs a new migration with `INSERT INTO player_alias ... ON CONFLICT DO NOTHING`.

### Formations

Templates (pitch x/y per position) live in `SeedSqlGenerator.FORMATIONS`: 4-4-1-1, 4-2-3-1, 4-3-1-2, 4-4-2,
4-4-2 (diamond), 3-4-2-1, 4-3-3, 4-1-4-1, 3-5-2, 3-4-3, 3-2-4-1. To add one: add a `Map.entry` with each position's
coordinates (x 0 = left touchline, y 0 = own goal; repeated positions right→left, mirrored around x=50 so the share
grid stays symmetrical), add it to the symmetry test in `frontend/src/app/state/view.spec.ts`, run tests.

### Extra "Did you mean" players (not in puzzles)

`data/wikidata-footballers.json` (Wikidata, CC0): footballers born ≥1962, ≥20 sitelinks, top 5,000 by fame, minus
non-footballers (no playing position + other occupation) and a hand `EXCLUDE` list (Viktor Orbán). Rebuild:
`python -I -X utf8 data/fetch_wikidata_players.py` (~20 min; Wikidata throttles). Loaded once by
`ExtraPlayersGenerator` as `V5__extra_players.sql` (merges puzzle players by name / multi-word alias / alt name, never
duplicates). To load a refreshed list, write a **new** migration (the generator refuses if `*__extra_players.sql` exists).

## Data rules

- Every match needs 2 independent sources before it ships.
- Good sources: UEFA squad-sheet PDFs, ESPN lineups, DFB data center, FBref, thesoccerworldcups.com, national-football-teams.com,
  club archives (lfchistory, fcbayern.com). 11v11 blocks automated fetches (403). Transfermarkt: manual tiebreaker only,
  never scraped. Wikipedia lineup tables don't survive automated fetches; use as a narrative cross-check.
- Drafting from memory is unreliable: in the 50-match pass ~1 in 4 drafts had a sub error and 3 had a wrong XI.
- Known source errors: ESPN lists Inter 2010's GK as "J. De León" (was Júlio César); FBref's minutes-based lineup puts Navas in Spain 2010's XI (Pedro started).
- Still single-source: Man Utd 2008, Inter 2010, Brazil 2002, Portugal 2016 subs.
- Puzzles #11–54 are `verified` by user decision (2026-11-12) on the strength of a web-search check (2+ agreeing
  sources cited, pages not read in full by a person); each carries a flag saying so.

## API

| Endpoint | Returns |
|---|---|
| `GET /api/plays/today?anonId=` | puzzle # + this device's play if Start was pressed (never creates one) |
| `GET /api/puzzle/today` | #, team, opponent, result, competition, date, formation, slots (position + coords only, **no names**) |
| `POST /api/plays` `{anonId}` | Start: playId, clock starts (idempotent per device/day; resume returns guesses) |
| `GET /api/players/suggest?q=` | one `{id, name, exact}` across all players, or 204 (<2 chars / no match) |
| `POST /api/plays/{id}/guesses` `{playerId}` (or `{text:""}` = timeout) | hit / miss / duplicate / timeout + slot, name, shortName, sub, tier, points |
| `GET /api/plays/{id}/reveal` | full pool with pick %, your picks, total (409 until guess 3) |

Config knobs (`backend/src/main/resources/application.yml`, `teamsheet.game.*`): guess-seconds 30, grace-seconds 2,
live-rarity-min-plays 200, sub-bonus 15, puzzle-zone America/Chicago.

## Roadmap

0. ~~ICE moonlighting/IP policy~~ checked, fine
1–4. ~~Data spike, mockup, data model, core loop~~ done
5. ~~Load ~60 matches + aliases, seed rarity~~ 54 live + 6 drafts; seed rarity still placeholder estimates
6. Share card ~~done~~ (native sheet + link card), streaks basic (no best-streak stat yet), pick logging done
7. **Friends beta — in progress** (started 2026-11-12). Watch `deploy/beta-stats.sh`
8. Launch: r/soccer, club subreddits, Product Hunt
9. Review ~2026-11-25: compare real pick % with seed rarity, tune scoring
10. Keep going if ~500 daily users, else shelve

Open ideas: weekly `pg_dump` backup script, GitHub Actions CI, best-streak/games-played stats, next batch of matches
before 2027-01-03, Mockup on claude.ai ("Teamsheet Mockup").
