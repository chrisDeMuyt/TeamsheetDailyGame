import { Injectable, signal } from '@angular/core';

const KEYS = {
  anonId: 'teamsheet.anonId',
  streak: 'teamsheet.streak',
  lastPlayed: 'teamsheet.lastPlayed',
} as const;

/** anonId, streak and last played puzzle # in localStorage. Survives storage being blocked (private mode). */
@Injectable({ providedIn: 'root' })
export class LocalStore {
  readonly streak = signal(this.readInt(KEYS.streak) ?? 0);
  readonly lastPlayed = signal(this.readInt(KEYS.lastPlayed));

  private memoryAnonId: string | null = null;

  anonId(): string {
    let id = this.read(KEYS.anonId) ?? this.memoryAnonId;
    if (!id) {
      id = crypto.randomUUID();
      this.write(KEYS.anonId, id);
    }
    this.memoryAnonId = id;
    return id;
  }

  /**
   * Call when a puzzle is finished. Idempotent per puzzle: the streak grows only when the previous
   * finished puzzle was the one right before this one, and resets to 1 after a gap.
   */
  recordFinish(puzzleNumber: number): void {
    const last = this.lastPlayed();
    if (last === puzzleNumber) return;
    const streak = last === puzzleNumber - 1 ? this.streak() + 1 : 1;
    this.streak.set(streak);
    this.lastPlayed.set(puzzleNumber);
    this.write(KEYS.streak, String(streak));
    this.write(KEYS.lastPlayed, String(puzzleNumber));
  }

  private readInt(key: string): number | null {
    const v = this.read(key);
    const n = v == null ? NaN : Number.parseInt(v, 10);
    return Number.isNaN(n) ? null : n;
  }

  private read(key: string): string | null {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  private write(key: string, value: string): void {
    try {
      localStorage.setItem(key, value);
    } catch {
      /* storage unavailable: keep the in-memory value */
    }
  }
}
