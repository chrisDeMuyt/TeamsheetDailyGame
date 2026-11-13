# Teamsheet (working title)

Daily soccer trivia game. Each day shows one historic match lineup; name 3 players from it. Rarer picks score more.

## Locked v1 decisions

| Area | Decision |
|---|---|
| Pool | Starting XI + subs who came on (no unused subs) |
| Era | Matches from 2000 onward |
| Picks | 3 per day, 30s each (server-side, +2s network grace), duplicates blocked |
| Matching | Fuzzy match + alias table; ambiguous names don't burn a guess |
| Scoring | Rarity tier by pick %: 40%+ = 10, 15–40% = 30, 5–15% = 60, 1–5% = 85, <1% = 100; +15 for subs |
| Cold start | Seed rarity from a proxy (pageviews/caps) until ~200 plays, then real pick % |
| UI | Formation on pitch + subs strip; slots show position only (no shirt numbers) |
| Reveal | Full pool with pick % under each name |
| Share | Mini pitch graphic with your 3 picks lit |
| Accounts | None; anon id in localStorage, one play per device per day |

## Files

- `teamsheet-seed.json` – 10 matches (XI + used subs), positions, minutes, verification status and sources
- `teamsheet-drafts.json` – 50 more lineups drafted from general knowledge, all `unverified`, with `draft_confidence` + `flags`. Never seeded. To promote one: verify against 2 sources, fill `sources`/subs/`num`, set verification, move it into `teamsheet-seed.json`, regenerate (new Vn migration, never edit applied ones). `DraftsValidationTest` checks structure.
- `schema.sql` – PostgreSQL schema; becomes Flyway migration `V1__init.sql`
- Mockup – Design canvas on claude.ai ("Teamsheet Mockup"), in-game and reveal screens

## Data rules (from the 10-match spike)

- Every match needs 2 independent sources before it ships
- Good sources: UEFA squad-sheet PDFs, ESPN match pages, DFB data center, FBref, club archives (lfchistory, fcbayern.com)
- Transfermarkt: manual tiebreaker only, never scraped (terms require written consent)
- Wikipedia lineup tables don't survive automated fetches; use as a narrative cross-check
- Known source errors: ESPN lists Inter 2010's GK as "J. De León" (was Júlio César); FBref's minutes-based lineup puts Navas in Spain's 2010 XI (Pedro started)
- Still single-source: Man Utd 2008, Inter 2010, Brazil 2002, Portugal 2016 subs

## API (v1)

| Endpoint | Returns |
|---|---|
| `GET /api/puzzle/today` | #, team, competition, date, formation, slots (position + coords only, no names) |
| `POST /api/plays` `{anonId}` | playId (idempotent per device/day) |
| `POST /api/plays/{id}/guesses` `{text}` | hit / miss / duplicate / ambiguous / timeout + slot, name, tier, points |
| `GET /api/plays/{id}/reveal` | full pool with pick %, your picks, total (after guess 3) |

## Roadmap

0. Check ICE moonlighting/IP policy
1. ~~Data spike (10 matches)~~ done
2. ~~UI mockup~~ done
3. ~~Data model + API sketch~~ done
4. Build core loop (Spring Boot + Postgres, Angular)
5. Load 60 matches + aliases, seed rarity
6. Share card, streaks, pick logging
7. Friends beta (10–20 people, 1 week)
8. Launch: r/soccer, club subreddits, Product Hunt
9. Review at 2 weeks: switch to real pick %, check scoring
10. Keep going if ~500 daily users, else shelve

## Local setup

```
docker run -d --name teamsheet-db -e POSTGRES_PASSWORD=dev -p 5432:5432 postgres:16
```
Spring Boot (start.spring.io): Web, Data JPA, PostgreSQL Driver, Flyway, Validation.
Copy `schema.sql` to `src/main/resources/db/migration/V1__init.sql`.
