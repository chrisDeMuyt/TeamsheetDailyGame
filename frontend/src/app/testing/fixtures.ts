import { PoolEntry, Puzzle, Reveal } from '../api/models';

/** Liverpool 2005, as the API serves it (slot order: starters by y then x, then subs). */
export const PUZZLE: Puzzle = {
  number: 1,
  team: 'Liverpool',
  competition: 'UEFA Champions League Final',
  date: '2005-05-25',
  formation: '4-4-1-1',
  slots: [
    { slot: 1, pos: 'GK', role: 'starter', x: 50, y: 5 },
    { slot: 2, pos: 'CB', role: 'starter', x: 38, y: 20 },
    { slot: 3, pos: 'CB', role: 'starter', x: 62, y: 20 },
    { slot: 4, pos: 'LB', role: 'starter', x: 15, y: 25 },
    { slot: 5, pos: 'RB', role: 'starter', x: 85, y: 25 },
    { slot: 6, pos: 'CM', role: 'starter', x: 40, y: 46 },
    { slot: 7, pos: 'CM', role: 'starter', x: 60, y: 46 },
    { slot: 8, pos: 'LM', role: 'starter', x: 15, y: 50 },
    { slot: 9, pos: 'RM', role: 'starter', x: 85, y: 50 },
    { slot: 10, pos: 'CAM', role: 'starter', x: 50, y: 67 },
    { slot: 11, pos: 'ST', role: 'starter', x: 50, y: 85 },
    { slot: 12, pos: 'MF', role: 'sub_used', x: null, y: null },
    { slot: 13, pos: 'MF', role: 'sub_used', x: null, y: null },
    { slot: 14, pos: 'FW', role: 'sub_used', x: null, y: null },
  ],
};

const NAMES = ['Dudek', 'Hyypiä', 'Carragher', 'Traoré', 'Finnan', 'Alonso', 'Gerrard', 'Riise', 'Luis García',
  'Kewell', 'Baroš', 'Šmicer', 'Hamann', 'Cissé'];

export function reveal(pickedSlots: number[], total = 0): Reveal {
  const pool: PoolEntry[] = PUZZLE.slots.map((s, i) => ({
    slot: s.slot, name: NAMES[i], shortName: NAMES[i], pos: s.pos, role: s.role, x: s.x, y: s.y,
    minuteOn: null, minuteOff: null, replacedSlot: null, pickPct: 10, picked: pickedSlots.includes(s.slot),
  }));
  return {
    puzzleNumber: 1, team: 'Liverpool', opponent: 'AC Milan', result: '3-3', competition: PUZZLE.competition,
    date: PUZZLE.date, formation: PUZZLE.formation, pctSource: 'seed', pool, totalScore: total,
    picks: [
      { guessIndex: 1, text: 'Traore', result: 'hit', slot: 4, shortName: 'Traoré', tier: 'Proper Fan', points: 60 },
      { guessIndex: 2, text: 'Diao', result: 'miss', points: 0 },
      { guessIndex: 3, text: '', result: 'timeout', points: 0 },
    ],
  };
}
