import { PUZZLE, reveal } from '../testing/fixtures';
import { Pick } from '../api/models';
import { TIER_EMOJI, buildShareText, columnFor, competitionShort, pickLabel, scoreline, formatClock, formatMatchDate, formatPct, playSpots } from './view';

describe('view helpers', () => {
  it('formats pick %, clock and dates like the mockup', () => {
    expect(formatPct(58)).toBe('58%');
    expect(formatPct(3.1)).toBe('3.1%');
    expect(formatPct(8.0)).toBe('8%');
    expect(formatClock(18_000)).toBe('0:18');
    expect(formatClock(29_200)).toBe('0:30');
    expect(formatClock(-5)).toBe('0:00');
    expect(formatMatchDate('2005-05-25')).toBe('25 May 2005');
    expect(competitionShort('UEFA Champions League Final')).toBe('CL Final');
  });

  it('formats the scoreline from the team side', () => {
    expect(scoreline('Chelsea', 'Bayern Munich', '1-1 aet, won 4-3 on penalties')).toBe('Chelsea 1–1 Bayern Munich (aet, 4–3 pens)');
    expect(scoreline('Germany', 'Brazil', 'lost 0-2')).toBe('Germany 0–2 Brazil');
    expect(scoreline('Spain', 'Germany', '1-0')).toBe('Spain 1–0 Germany');
    expect(scoreline('South Korea', 'Italy', '2-1 aet (golden goal)')).toBe('South Korea 2–1 Italy (aet, golden goal)');
    expect(scoreline('Deportivo', 'AC Milan', '4-0 (lost first leg 1-4)')).toBe('Deportivo 4–0 AC Milan (lost first leg 1–4)');
    expect(scoreline('Netherlands', 'Spain', 'lost 0-1 aet')).toBe('Netherlands 0–1 Spain (aet)');
    expect(scoreline('Milan', 'Liverpool', '3-3 aet, lost 2-3 on penalties')).toBe('Milan 3–3 Liverpool (aet, 2–3 pens)');
  });

  it('pitch shows short names; the guess list below shows full names', () => {
    const hit: Pick = { guessIndex: 1, text: 'John Terry', result: 'hit', slot: 4, name: 'John Terry', shortName: 'Terry', tier: 'Solid Shout', points: 30 };
    expect(playSpots(PUZZLE.slots, [hit]).find((s) => s.slot === 4)?.label).toBe('Terry');
    expect(pickLabel(hit)).toBe('John Terry');
  });

  it('shows positions only until a slot is hit', () => {
    const spots = playSpots(PUZZLE.slots, [
      { guessIndex: 1, text: 'traore', result: 'hit', slot: 4, shortName: 'Traoré', tier: 'Cult Hero', points: 100 },
      { guessIndex: 2, text: 'Diao', result: 'miss', points: 0 },
    ]);
    expect(spots.find((s) => s.slot === 4)).toMatchObject({ state: 'hit', label: 'Traoré', points: 100 });
    expect(spots.filter((s) => s.state === 'empty').map((s) => s.label)).toContain('ST');
    expect(spots.filter((s) => s.sub)).toHaveLength(3);
  });

  it('has an emoji for every tier and never names tiers in the share text', () => {
    expect(Object.keys(TIER_EMOJI)).toEqual(['Tap-in', 'Solid Shout', 'Proper Fan', 'Hidden Gem', 'Cult Hero']);
    const text = buildShareText(reveal([4], 60), 1);
    for (const tier of Object.keys(TIER_EMOJI)) expect(text).not.toContain(tier);
  });

  it('maps pitch x to mirrored columns', () => {
    expect([15, 38, 40, 50, 60, 62, 85].map(columnFor)).toEqual([0, 1, 1, 2, 3, 3, 4]);
    expect([25, 50, 75].map(columnFor)).toEqual([1, 2, 3]);   // back three
    expect([18, 82, 12, 88].map(columnFor)).toEqual([0, 4, 0, 4]);
  });

  it('keeps every formation row symmetrical (all templates used by the seed)', () => {
    // Starter x/y per formation, as SeedSqlGenerator places them.
    const formations: Record<string, [number, number][]> = {
      '4-4-1-1': [[50, 5], [85, 25], [62, 20], [38, 20], [15, 25], [85, 50], [60, 46], [40, 46], [15, 50], [50, 67], [50, 85]],
      '4-2-3-1': [[50, 5], [85, 25], [62, 20], [38, 20], [15, 25], [62, 42], [38, 42], [82, 66], [50, 64], [18, 66], [50, 85]],
      '4-3-1-2': [[50, 5], [85, 25], [62, 20], [38, 20], [15, 25], [50, 40], [72, 50], [28, 50], [50, 65], [62, 84], [38, 84]],
      '4-4-2': [[50, 5], [85, 25], [62, 20], [38, 20], [15, 25], [85, 52], [60, 48], [40, 48], [15, 52], [62, 82], [38, 82]],
      '4-4-2 (diamond)': [[50, 5], [85, 25], [62, 20], [38, 20], [15, 25], [50, 40], [75, 52], [25, 52], [50, 65], [62, 84], [38, 84]],
      '3-4-2-1': [[50, 5], [75, 22], [50, 20], [25, 22], [88, 48], [60, 45], [40, 45], [12, 48], [65, 66], [35, 66], [50, 85]],
      '4-3-3': [[50, 5], [85, 25], [62, 20], [38, 20], [15, 25], [50, 40], [68, 52], [32, 52], [82, 72], [50, 85], [18, 72]],
      '4-1-4-1': [[50, 5], [85, 25], [62, 20], [38, 20], [15, 25], [50, 38], [85, 56], [62, 55], [38, 55], [15, 56], [50, 85]],
      '3-5-2': [[50, 5], [75, 22], [50, 20], [25, 22], [88, 48], [68, 52], [50, 44], [32, 52], [12, 48], [62, 82], [38, 82]],
      '3-4-3': [[50, 5], [75, 22], [50, 20], [25, 22], [88, 48], [60, 45], [40, 45], [12, 48], [80, 74], [50, 85], [20, 74]],
      '3-2-4-1': [[50, 5], [75, 22], [50, 20], [25, 22], [62, 40], [38, 40], [85, 66], [62, 64], [38, 64], [15, 66], [50, 85]],
    };
    for (const [name, xy] of Object.entries(formations)) {
      const r = reveal([], 0);
      r.pool = xy.map(([x, y], i) => ({ ...r.pool[0], slot: i + 1, role: 'starter' as const, x, y, picked: false }));
      const rows = buildShareText(r, 0).split('\n').filter((l) => /^[🟩⚪🟡]+$/u.test(l));
      expect(rows.length, name).toBeGreaterThan(3);
      for (const row of rows) {
        const cells = [...row];
        expect(cells.join(''), `${name} row not symmetrical`).toBe([...cells].reverse().join(''));
      }
    }
  });

  it('builds a mini-pitch share text with picks lit, attack on top', () => {
    const text = buildShareText(reveal([4, 12], 135), 3);
    const lines = text.split('\n');
    expect(lines[0]).toBe('Teamsheet #1 · 135 pts');
    // ST, CAM, midfield four (46 and 50 merge into one row), back four, GK
    expect(lines.slice(1, 6)).toEqual([
      '🟩🟩⚪🟩🟩',
      '🟩🟩⚪🟩🟩',
      '⚪⚪🟩⚪⚪',
      '🟡⚪🟩⚪⚪',
      '🟩🟩⚪🟩🟩',
    ]);
    expect(lines[6]).toBe('Subs 🟡⚪⚪');
    expect(lines[7]).toBe('🪭 ❌ ⏱️');
    expect(lines[8]).toBe('🔥 3-day streak');
    expect(lines).toHaveLength(9); // no link line unless one is given
  });

  it('ends with a link to the game when given one', () => {
    const lines = buildShareText(reveal([4], 60), 1, 'https://teamsheet-daily.fly.dev').split('\n');
    expect(lines.at(-1)).toBe('https://teamsheet-daily.fly.dev');
    expect(lines.at(-2)).toBe('🪭 ❌ ⏱️');   // streak line omitted at 1 day
  });
});
