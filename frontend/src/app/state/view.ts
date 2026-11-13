import { Pick, PoolEntry, Reveal, Slot } from '../api/models';

/** One circle on the pitch or the subs strip. */
export interface Spot {
  slot: number;
  pos: string;
  sub: boolean;
  x: number | null;
  y: number | null;
  /** empty = not guessed yet; hit = your pick during play; revealed / picked = after the game. */
  state: 'empty' | 'hit' | 'revealed' | 'picked';
  label: string;        // position (empty) or short name
  detail?: string;      // pick % after reveal
  points?: number;      // shown inside the circle on a hit
}

export function playSpots(slots: Slot[], picks: Pick[]): Spot[] {
  const hits = new Map(picks.filter((p) => p.result === 'hit' && p.slot).map((p) => [p.slot!, p]));
  return slots.map((s) => {
    const hit = hits.get(s.slot);
    return {
      slot: s.slot,
      pos: s.pos,
      sub: s.role === 'sub_used',
      x: s.x,
      y: s.y,
      state: hit ? 'hit' : 'empty',
      label: hit ? (hit.shortName ?? hit.name ?? '') : s.pos,
      points: hit?.points,
    };
  });
}

export function revealSpots(pool: PoolEntry[]): Spot[] {
  return pool.map((p) => ({
    slot: p.slot,
    pos: p.pos,
    sub: p.role === 'sub_used',
    x: p.x,
    y: p.y,
    state: p.picked ? 'picked' : 'revealed',
    label: p.shortName,
    detail: formatPct(p.pickPct),
  }));
}

/** 58 -> "58%", 3.1 -> "3.1%", 8.0 -> "8%". */
export function formatPct(pct: number): string {
  const rounded = Math.round(pct * 10) / 10;
  return `${Number.isInteger(rounded) ? rounded : rounded.toFixed(1)}%`;
}

/** "UEFA Champions League Final" -> "Champions League Final". */
export function competitionLong(c: string): string {
  return c.replace(/^(UEFA|FIFA)\s+/, '');
}

/** "UEFA Champions League Final" -> "CL Final" (header subtitle). */
export function competitionShort(c: string): string {
  return competitionLong(c).replace('Champions League', 'CL').replace('World Cup', 'WC');
}

export function formatMatchDate(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number);
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  return `${d} ${months[m - 1]} ${y}`;
}

/** "0:18" */
export function formatClock(ms: number): string {
  const s = Math.max(0, Math.ceil(ms / 1000));
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
}

/** Points split for display: a sub's +15 bonus shown separately ("Proper Fan 60 + 15"). */
export const SUB_BONUS = 15;

export function pickLabel(p: Pick): string {
  if (p.result === 'hit') return p.shortName ?? p.name ?? p.text;
  return p.text.trim() || '—';
}

export function pickOutcome(p: Pick): string {
  if (p.result === 'miss') return 'Not in squad';
  if (p.result === 'timeout') return "Time's up";
  return p.tier ?? '';
}

// ---------------------------------------------------------------- share text

/** One emoji per tier for the share text, so it never spells out tier names. */
export const TIER_EMOJI: Record<string, string> = {
  'Tap-in': '🥅',
  'Solid Shout': '📣',
  'Proper Fan': '🪭',
  'Hidden Gem': '💎',
  'Cult Hero': '🐐',
};
const MISS = '❌';
const TIMEOUT = '⏱️';

const GRASS = '🟩';
const PLAYER = '⚪';
const LIT = '🟡';
const COLS = 5;

/**
 * Pitch x (0-100) -> one of 5 columns, rounding away from the centre so pairs mirror each other:
 * CMs at 40/60 -> columns 1 and 3 (⚪⚪🟩⚪⚪), not 2 and 3.
 */
export function columnFor(x: number): number {
  const offset = (x - 50) / 20;
  const rounded = Math.sign(offset) * Math.round(Math.abs(offset));
  return Math.min(COLS - 1, Math.max(0, 2 + rounded));
}

/**
 * Mini pitch with your picks lit, attack at the top:
 *   Teamsheet #1 · 185 pts
 *   🟩🟩⚪🟩🟩
 *   ...
 *   Subs ⚪🟡⚪
 */
export function buildShareText(reveal: Reveal, streak: number, link?: string): string {
  const starters = reveal.pool.filter((p) => p.role === 'starter' && p.x != null && p.y != null);
  const subs = reveal.pool.filter((p) => p.role === 'sub_used');

  // Group starters into rows: sort by y (attack first) and merge players within 6 units of a row's first y.
  const sorted = [...starters].sort((a, b) => b.y! - a.y!);
  const rows: PoolEntry[][] = [];
  for (const p of sorted) {
    const row = rows[rows.length - 1];
    if (row && row[0].y! - p.y! <= 6) row.push(p);
    else rows.push([p]);
  }

  const lines = rows.map((row) => {
    const cells: string[] = Array(COLS).fill(GRASS);
    for (const p of [...row].sort((a, b) => a.x! - b.x!)) {
      let col = columnFor(p.x!);
      if (cells[col] !== GRASS) {
        // Collision: move outward from the centre first, so mirrored players stay mirrored.
        const away = p.x! >= 50 ? 1 : -1;
        const tries = [col + away, col - away, col + 2 * away, col - 2 * away];
        col = tries.find((c) => c >= 0 && c < COLS && cells[c] === GRASS) ?? col;
      }
      cells[col] = p.picked ? LIT : PLAYER;
    }
    return cells.join('');
  });

  const subLine = subs.length ? `Subs ${subs.map((s) => (s.picked ? LIT : PLAYER)).join('')}` : '';
  const results = reveal.picks
    .map((p) => (p.result === 'hit' ? (TIER_EMOJI[p.tier ?? ''] ?? '✅') : p.result === 'timeout' ? TIMEOUT : MISS))
    .join(' ');

  return [
    `Teamsheet #${reveal.puzzleNumber} · ${reveal.totalScore} pts`,
    ...lines,
    subLine,
    results,
    streak > 1 ? `🔥 ${streak}-day streak` : '',
    link ?? '',
  ]
    .filter(Boolean)
    .join('\n');
}
