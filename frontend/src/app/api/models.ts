/** Mirrors the backend DTOs (com.teamsheet.api.Dtos). */

export type Role = 'starter' | 'sub_used';

export interface Slot {
  slot: number;
  pos: string;
  role: Role;
  x: number | null;
  y: number | null;
}

export interface Puzzle {
  number: number;
  team: string;
  competition: string;
  date: string; // ISO date
  formation: string;
  slots: Slot[];
}

export type PickResult = 'hit' | 'miss' | 'timeout';

/** A guess that consumed one of the 3 attempts. */
export interface Pick {
  guessIndex: number;
  text: string;
  result: PickResult;
  slot?: number;
  name?: string;
  shortName?: string;
  sub?: boolean;
  tier?: string;
  points: number;
}

export interface PlayView {
  playId: string;
  puzzleNumber: number;
  guessesUsed: number;
  guessesLeft: number;
  finished: boolean;
  totalScore: number;
  deadline: string | null;
  guesses: Pick[];
}

export type GuessResultType = 'hit' | 'miss' | 'duplicate' | 'ambiguous' | 'timeout';

export interface GuessResult {
  result: GuessResultType;
  guessIndex?: number;
  guessesLeft: number;
  slot?: number;
  name?: string;
  shortName?: string;
  sub?: boolean;
  tier?: string;
  points: number;
  totalScore: number;
  finished: boolean;
  deadline?: string;
  message?: string;
}

export interface PoolEntry {
  slot: number;
  name: string;
  shortName: string;
  pos: string;
  role: Role;
  x: number | null;
  y: number | null;
  minuteOn: number | null;
  minuteOff: number | null;
  replacedSlot: number | null;
  pickPct: number;
  picked: boolean;
}

export interface Reveal {
  puzzleNumber: number;
  team: string;
  opponent: string;
  result: string;
  competition: string;
  date: string;
  formation: string;
  pctSource: 'seed' | 'live';
  pool: PoolEntry[];
  picks: Pick[];
  totalScore: number;
}
