import { PUZZLE, reveal } from '../testing/fixtures';
import { buildShareText, competitionShort, formatClock, formatMatchDate, formatPct, playSpots } from './view';

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

  it('shows positions only until a slot is hit', () => {
    const spots = playSpots(PUZZLE.slots, [
      { guessIndex: 1, text: 'traore', result: 'hit', slot: 4, shortName: 'Traoré', tier: 'Cult Hero', points: 100 },
      { guessIndex: 2, text: 'Diao', result: 'miss', points: 0 },
    ]);
    expect(spots.find((s) => s.slot === 4)).toMatchObject({ state: 'hit', label: 'Traoré', points: 100 });
    expect(spots.filter((s) => s.state === 'empty').map((s) => s.label)).toContain('ST');
    expect(spots.filter((s) => s.sub)).toHaveLength(3);
  });

  it('builds a mini-pitch share text with picks lit, attack on top', () => {
    const text = buildShareText(reveal([4, 12], 135), 3);
    const lines = text.split('\n');
    expect(lines[0]).toBe('Teamsheet #1 · 135 pts');
    // ST, CAM, midfield four (46 and 50 merge into one row), back four, GK
    expect(lines.slice(1, 6)).toEqual([
      '🟩🟩⚪🟩🟩',
      '🟩🟩⚪🟩🟩',
      '⚪🟩⚪⚪⚪',
      '🟡⚪🟩⚪⚪',
      '🟩🟩⚪🟩🟩',
    ]);
    expect(lines[6]).toBe('Subs 🟡⚪⚪');
    expect(lines[7]).toBe('✅ Proper Fan · ❌ · ⏱️');
    expect(lines[8]).toBe('🔥 3-day streak');
  });
});
