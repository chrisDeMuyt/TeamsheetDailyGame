-- Reschedule from puzzle #3 (2026-11-13), user decision 2026-11-13:
--   * today (#3) is now 2022-wc-final-argentina (was 2004-euro-final-greece; 1 play on it is discarded)
--   * 7 very obscure matches unscheduled (match rows kept; see teamsheet-shelved.json)
--   * later puzzles renumbered and re-dated: one per day, no gaps
-- Format is parsed by SeedSqlGenerator.scan (DELETE ... >= N, then puzzle rows). Do not edit by hand.

DELETE FROM pick_stat WHERE puzzle_number >= 3;
DELETE FROM play WHERE puzzle_number >= 3;
DELETE FROM puzzle WHERE number >= 3;

INSERT INTO puzzle (number, puzzle_date, match_id) VALUES
  (3, DATE '2026-11-13', '2022-wc-final-argentina'),
  (4, DATE '2026-11-14', '2010-wc-final-spain'),
  (5, DATE '2026-11-15', '2014-wc-semi-germany'),
  (6, DATE '2026-11-16', '2008-ucl-final-man-utd'),
  (7, DATE '2026-11-17', '2010-ucl-final-inter'),
  (8, DATE '2026-11-18', '2016-euro-final-portugal'),
  (9, DATE '2026-11-19', '2006-wc-final-italy'),
  (10, DATE '2026-11-20', '2002-wc-final-brazil'),
  (11, DATE '2026-11-21', '2006-fa-cup-final-liverpool'),
  (12, DATE '2026-11-22', '2023-ucl-final-man-city'),
  (13, DATE '2026-11-23', '2011-pl-man-utd'),
  (14, DATE '2026-11-24', '2002-ucl-final-real-madrid'),
  (15, DATE '2026-11-25', '2018-wc-semi-croatia'),
  (16, DATE '2026-11-26', '2011-ucl-final-barcelona'),
  (17, DATE '2026-11-27', '2025-ucl-final-psg'),
  (18, DATE '2026-11-28', '2010-ucl-final-bayern'),
  (19, DATE '2026-11-29', '2004-pl-arsenal'),
  (20, DATE '2026-11-30', '2016-euro-qf-wales'),
  (21, DATE '2026-12-01', '2024-euro-final-spain'),
  (22, DATE '2026-12-02', '2020-ucl-final-bayern'),
  (23, DATE '2026-12-03', '2003-ucl-final-milan'),
  (24, DATE '2026-12-04', '2008-ucl-final-chelsea'),
  (25, DATE '2026-12-05', '2012-dfb-pokal-final-dortmund'),
  (26, DATE '2026-12-06', '2019-ucl-semi-liverpool'),
  (27, DATE '2026-12-07', '2014-wc-semi-brazil'),
  (28, DATE '2026-12-08', '2008-euro-final-spain'),
  (29, DATE '2026-12-09', '2004-euro-final-portugal'),
  (30, DATE '2026-12-10', '2018-wc-final-france'),
  (31, DATE '2026-12-11', '2010-wc-final-netherlands'),
  (32, DATE '2026-12-12', '2014-wc-final-germany'),
  (33, DATE '2026-12-13', '2001-ucl-final-bayern'),
  (34, DATE '2026-12-14', '2019-ucl-semi-tottenham'),
  (35, DATE '2026-12-15', '2005-ucl-final-milan'),
  (36, DATE '2026-12-16', '2019-ucl-r16-ajax'),
  (37, DATE '2026-12-17', '2015-ucl-final-barcelona'),
  (38, DATE '2026-12-18', '2021-ucl-final-chelsea'),
  (39, DATE '2026-12-19', '2012-pl-man-city'),
  (40, DATE '2026-12-20', '2016-euro-final-france'),
  (41, DATE '2026-12-21', '2017-ucl-final-real-madrid'),
  (42, DATE '2026-12-22', '2006-ucl-final-barcelona'),
  (43, DATE '2026-12-23', '2013-ucl-final-bayern'),
  (44, DATE '2026-12-24', '2002-wc-final-germany'),
  (45, DATE '2026-12-25', '2006-wc-final-france'),
  (46, DATE '2026-12-26', '2016-pl-leicester'),
  (47, DATE '2026-12-27', '2014-ucl-final-real-madrid');
