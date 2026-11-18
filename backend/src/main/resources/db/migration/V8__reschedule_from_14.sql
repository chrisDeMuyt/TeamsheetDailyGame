-- Reschedule from puzzle #14 (2026-11-24), user decision 2026-11-17: mix the two World Cup 2026 sides added in V7
-- as #56-57 in earlier:
--   * #14 = 2026-wc-final-argentina (36 days before the Spain side of the same final)
--   * #25 = 2026-wc-r16-norway (29 days before the Brazil side of the same match)
--   * every puzzle from #14 on moves back one or two days; no plays exist yet for #14+ (today is #7)
-- Format is parsed by SeedSqlGenerator.scan (DELETE ... >= N, then puzzle rows). Do not edit by hand.

DELETE FROM pick_stat WHERE puzzle_number >= 14;
DELETE FROM play WHERE puzzle_number >= 14;
DELETE FROM puzzle WHERE number >= 14;

INSERT INTO puzzle (number, puzzle_date, match_id) VALUES
  (14, DATE '2026-11-24', '2026-wc-final-argentina'),
  (15, DATE '2026-11-25', '2002-ucl-final-real-madrid'),
  (16, DATE '2026-11-26', '2018-wc-semi-croatia'),
  (17, DATE '2026-11-27', '2011-ucl-final-barcelona'),
  (18, DATE '2026-11-28', '2025-ucl-final-psg'),
  (19, DATE '2026-11-29', '2010-ucl-final-bayern'),
  (20, DATE '2026-11-30', '2004-pl-arsenal'),
  (21, DATE '2026-12-01', '2016-euro-qf-wales'),
  (22, DATE '2026-12-02', '2024-euro-final-spain'),
  (23, DATE '2026-12-03', '2020-ucl-final-bayern'),
  (24, DATE '2026-12-04', '2003-ucl-final-milan'),
  (25, DATE '2026-12-05', '2026-wc-r16-norway'),
  (26, DATE '2026-12-06', '2008-ucl-final-chelsea'),
  (27, DATE '2026-12-07', '2012-dfb-pokal-final-dortmund'),
  (28, DATE '2026-12-08', '2019-ucl-semi-liverpool'),
  (29, DATE '2026-12-09', '2014-wc-semi-brazil'),
  (30, DATE '2026-12-10', '2008-euro-final-spain'),
  (31, DATE '2026-12-11', '2004-euro-final-portugal'),
  (32, DATE '2026-12-12', '2018-wc-final-france'),
  (33, DATE '2026-12-13', '2010-wc-final-netherlands'),
  (34, DATE '2026-12-14', '2014-wc-final-germany'),
  (35, DATE '2026-12-15', '2001-ucl-final-bayern'),
  (36, DATE '2026-12-16', '2019-ucl-semi-tottenham'),
  (37, DATE '2026-12-17', '2005-ucl-final-milan'),
  (38, DATE '2026-12-18', '2019-ucl-r16-ajax'),
  (39, DATE '2026-12-19', '2015-ucl-final-barcelona'),
  (40, DATE '2026-12-20', '2021-ucl-final-chelsea'),
  (41, DATE '2026-12-21', '2012-pl-man-city'),
  (42, DATE '2026-12-22', '2016-euro-final-france'),
  (43, DATE '2026-12-23', '2017-ucl-final-real-madrid'),
  (44, DATE '2026-12-24', '2006-ucl-final-barcelona'),
  (45, DATE '2026-12-25', '2013-ucl-final-bayern'),
  (46, DATE '2026-12-26', '2002-wc-final-germany'),
  (47, DATE '2026-12-27', '2006-wc-final-france'),
  (48, DATE '2026-12-28', '2016-pl-leicester'),
  (49, DATE '2026-12-29', '2014-ucl-final-real-madrid'),
  (50, DATE '2026-12-30', '2026-wc-final-spain'),
  (51, DATE '2026-12-31', '2026-wc-semi-england'),
  (52, DATE '2027-01-01', '2026-wc-r32-germany'),
  (53, DATE '2027-01-02', '2026-wc-qf-france'),
  (54, DATE '2027-01-03', '2026-wc-r16-brazil'),
  (55, DATE '2027-01-04', '2026-wc-r16-portugal'),
  (56, DATE '2027-01-05', '2026-wc-r32-netherlands'),
  (57, DATE '2027-01-06', '2026-wc-r16-belgium');
