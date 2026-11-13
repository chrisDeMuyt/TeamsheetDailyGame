-- Teamsheet beta stats. Read-only. Run via deploy/beta-stats.sh
\pset footer off
\pset null '-'
-- Test devices to leave out (smoke tests during deploys)
\set test_devices '''00000000-0000-4000-8000-0000000000aa'''
BEGIN TRANSACTION READ ONLY;

\echo
\echo '=== Overview ==='
SELECT count(DISTINCT anon_id) AS devices, count(*) AS plays, count(*) FILTER (WHERE finished_at IS NOT NULL) AS finished, round(100.0 * count(*) FILTER (WHERE finished_at IS NOT NULL) / NULLIF(count(*), 0)) AS "finish %", round(avg(total_score) FILTER (WHERE finished_at IS NOT NULL), 1) AS "avg score" FROM play WHERE anon_id NOT IN (:test_devices);

\echo '=== By puzzle (newest first) ==='
SELECT p.puzzle_number AS "#", z.puzzle_date AS date, m.team, count(*) AS plays, count(*) FILTER (WHERE p.finished_at IS NOT NULL) AS finished, round(avg(p.total_score) FILTER (WHERE p.finished_at IS NOT NULL), 1) AS "avg score", max(p.total_score) AS best FROM play p JOIN puzzle z ON z.number = p.puzzle_number JOIN match m ON m.id = z.match_id WHERE p.anon_id NOT IN (:test_devices) GROUP BY 1, 2, 3 ORDER BY 1 DESC LIMIT 14;

\echo '=== Returning devices (days played per device) ==='
SELECT days AS "days played", count(*) AS devices FROM (SELECT anon_id, count(*) AS days FROM play WHERE anon_id NOT IN (:test_devices) GROUP BY anon_id) t GROUP BY 1 ORDER BY 1;

\echo '=== Guess outcomes ==='
SELECT g.result, count(*) AS guesses, round(100.0 * count(*) / sum(count(*)) OVER (), 1) AS "%" FROM guess g JOIN play p ON p.id = g.play_id WHERE p.anon_id NOT IN (:test_devices) GROUP BY 1 ORDER BY 2 DESC;

\echo '=== Score spread (finished games) ==='
SELECT CASE WHEN total_score = 0 THEN '0' WHEN total_score <= 50 THEN '1-50' WHEN total_score <= 100 THEN '51-100' WHEN total_score <= 150 THEN '101-150' WHEN total_score <= 200 THEN '151-200' ELSE '201+' END AS score, count(*) AS games FROM play WHERE finished_at IS NOT NULL AND anon_id NOT IN (:test_devices) GROUP BY 1 ORDER BY min(total_score);

\echo '=== Most picked players (last 7 puzzles, top 3 each) ==='
SELECT "#", player, picks, tier FROM (SELECT p.puzzle_number AS "#", pl.display_name AS player, count(*) AS picks, min(g.tier) AS tier, row_number() OVER (PARTITION BY p.puzzle_number ORDER BY count(*) DESC, pl.display_name) AS rk FROM guess g JOIN play p ON p.id = g.play_id JOIN player pl ON pl.id = g.player_id WHERE g.result = 'hit' AND p.anon_id NOT IN (:test_devices) AND p.puzzle_number > (SELECT max(puzzle_number) - 7 FROM play) GROUP BY 1, 2) t WHERE rk <= 3 ORDER BY "#" DESC, picks DESC;

\echo '=== Most common wrong guesses (are aliases missing?) ==='
SELECT g.raw_text AS guessed, count(*) AS times FROM guess g JOIN play p ON p.id = g.play_id WHERE g.result = 'miss' AND p.anon_id NOT IN (:test_devices) GROUP BY 1 ORDER BY 2 DESC, 1 LIMIT 15;

ROLLBACK;
\q
