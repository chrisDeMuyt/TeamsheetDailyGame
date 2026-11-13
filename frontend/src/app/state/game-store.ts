import { HttpErrorResponse } from '@angular/common/http';
import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';
import { ApiService, Timed } from '../api/api.service';
import { GuessResult, GuessResultType, Pick, PlayView, PlayerOption, Puzzle, Reveal } from '../api/models';
import { LocalStore } from './local-store';
import { playSpots, revealSpots } from './view';

/** intro = the Start screen: nothing about today's match is loaded and no clock is running yet. */
export type Phase = 'loading' | 'error' | 'intro' | 'playing' | 'revealed';

export interface Feedback {
  kind: GuessResultType | 'error' | 'notfound' | 'suggest';
  text: string;
}

export const GUESSES = 3;
export const GUESS_MS = 30_000;
export const MIN_CHARS = 2;

/**
 * The whole game flow: Start screen -> load today + create play (clock starts) -> 3 guesses -> reveal.
 * Guesses are players: Enter submits only an exact full name; anything else offers one "Did you mean".
 * The server owns the clock; we mirror its deadline and send an empty guess when it runs out.
 */
@Injectable({ providedIn: 'root' })
export class GameStore {
  private readonly api = inject(ApiService);
  private readonly local = inject(LocalStore);

  readonly phase = signal<Phase>('loading');
  readonly error = signal<string | null>(null);
  readonly introNumber = signal<number | null>(null);
  readonly starting = signal(false);
  readonly puzzle = signal<Puzzle | null>(null);
  readonly playId = signal<string | null>(null);
  readonly picks = signal<Pick[]>([]);
  readonly score = signal(0);
  readonly reveal = signal<Reveal | null>(null);
  readonly feedback = signal<Feedback | null>(null);
  readonly submitting = signal(false);
  /** The "Did you mean" player offered after a non-exact Enter. */
  readonly suggestion = signal<PlayerOption | null>(null);
  /** Slot that was just hit, for the pop animation. */
  readonly poppedSlot = signal<number | null>(null);

  /** Deadline for the open guess, on the local clock. */
  private readonly deadline = signal<number | null>(null);
  readonly now = signal(Date.now());

  readonly remainingMs = computed(() => {
    const d = this.deadline();
    return d == null ? 0 : Math.max(0, d - this.now());
  });
  readonly guessNumber = computed(() => Math.min(this.picks().length + 1, GUESSES));

  readonly spots = computed(() => {
    const r = this.reveal();
    if (r) return revealSpots(r.pool);
    const p = this.puzzle();
    return p ? playSpots(p.slots, this.picks()) : [];
  });
  readonly starters = computed(() => this.spots().filter((s) => !s.sub));
  readonly subs = computed(() => this.spots().filter((s) => s.sub));

  readonly streak = this.local.streak;
  readonly lastPlayed = this.local.lastPlayed;

  /** Pause before switching to the reveal so the last pick's pop is visible. Tests set 0. */
  revealDelayMs = 1400;

  private ticker: ReturnType<typeof setInterval> | null = null;
  private timeoutSentFor: number | null = null;

  constructor() {
    inject(DestroyRef).onDestroy(() => this.stopTicker());
  }

  /** On load: Start screen for a fresh day; straight back into a started or finished game otherwise. */
  async init(): Promise<void> {
    this.phase.set('loading');
    this.error.set(null);
    try {
      const { body: status, serverNow } = await this.api.todayStatus(this.local.anonId());
      this.introNumber.set(status.puzzleNumber);
      if (!status.play) {
        this.phase.set('intro');
        return;
      }
      await this.enter(status.play, serverNow);
    } catch (e) {
      this.fail(e);
    }
  }

  /** The Start button: only now is the match loaded and the play created (the 30s clock starts server-side). */
  async start(): Promise<void> {
    if (this.starting() || this.phase() !== 'intro') return;
    this.starting.set(true);
    try {
      this.puzzle.set((await this.api.today()).body);
      const { body: play, serverNow } = await this.api.createPlay(this.local.anonId());
      await this.enter(play, serverNow);
    } catch (e) {
      this.fail(e);
    } finally {
      this.starting.set(false);
    }
  }

  private async enter(play: PlayView, serverNow: number): Promise<void> {
    this.playId.set(play.playId);
    this.picks.set(play.guesses ?? []);
    this.score.set(play.totalScore);
    if (play.finished) {
      await this.finish();
      return;
    }
    if (!this.puzzle()) this.puzzle.set((await this.api.today()).body);
    this.setDeadline(play.deadline, serverNow);
    this.phase.set('playing');
    this.startTicker();
  }

  private fail(e: unknown): void {
    this.phase.set('error');
    this.error.set(
      e instanceof HttpErrorResponse && e.status === 404
        ? 'No puzzle today. Check back tomorrow.'
        : "Couldn't reach the Teamsheet server.",
    );
  }

  /**
   * Enter / Submit with typed text. An exact full name is guessed straight away; otherwise we offer one
   * "Did you mean" (no guess used). Returns the guess result, or null if no guess was made.
   */
  async submitText(text: string): Promise<GuessResultType | null> {
    const typed = text.trim();
    if (this.phase() !== 'playing' || this.submitting()) return null;
    if (typed.length < MIN_CHARS) {
      this.feedback.set({ kind: 'notfound', text: `Type at least ${MIN_CHARS} letters.` });
      return null;
    }
    this.submitting.set(true);
    let option: PlayerOption | null;
    try {
      option = await this.api.suggest(typed);
    } catch {
      this.feedback.set({ kind: 'error', text: "Couldn't check that name. Try again." });
      return null;
    } finally {
      this.submitting.set(false);
    }
    if (!option) {
      this.suggestion.set(null);
      this.feedback.set({ kind: 'notfound', text: `No player called "${typed}". Guess not used.` });
      return null;
    }
    if (option.exact) return this.submitPlayer(option);
    this.suggestion.set(option);
    this.feedback.set({ kind: 'suggest', text: 'Not an exact name. Guess not used yet.' });
    return null;
  }

  /** Tap on "Did you mean". */
  async pickSuggestion(): Promise<GuessResultType | null> {
    const s = this.suggestion();
    return s ? this.submitPlayer(s) : null;
  }

  /** The user edited the text: an old suggestion no longer applies. */
  clearSuggestion(): void {
    this.suggestion.set(null);
  }

  submitPlayer(option: PlayerOption): Promise<GuessResultType | null> {
    return this.send(option.name, (id) => this.api.guessPlayer(id, option.id));
  }

  /** Empty text = "time ran out" (the server records a timeout). */
  submit(text: string): Promise<GuessResultType | null> {
    return this.send(text, (id) => this.api.guess(id, text));
  }

  private async send(text: string, call: (playId: string) => Promise<Timed<GuessResult>>): Promise<GuessResultType | null> {
    const id = this.playId();
    if (this.phase() !== 'playing' || this.submitting() || !id) return null;
    this.submitting.set(true);
    try {
      const { body, serverNow } = await call(id);
      this.suggestion.set(null);
      this.apply(body, text, serverNow);
      return body.result;
    } catch (e) {
      if (e instanceof HttpErrorResponse && e.status === 409) {
        await this.finish(); // already finished elsewhere (another tab)
      } else {
        this.feedback.set({ kind: 'error', text: "Couldn't send that guess. Try again." });
      }
      return null;
    } finally {
      this.submitting.set(false);
    }
  }

  /** Called on every tick; fires the timeout once per deadline. */
  tick(nowMs = Date.now()): void {
    this.now.set(nowMs);
    const d = this.deadline();
    if (this.phase() !== 'playing' || d == null || nowMs < d) return;
    if (this.submitting() || this.timeoutSentFor === d) return;
    this.timeoutSentFor = d;
    void this.submit('');
  }

  private apply(r: GuessResult, text: string, serverNow: number): void {
    this.score.set(r.totalScore);
    const consumed = r.result === 'hit' || r.result === 'miss' || r.result === 'timeout';
    if (consumed) {
      const pick: Pick = {
        guessIndex: r.guessIndex ?? this.picks().length + 1,
        text,
        result: r.result as Pick['result'],
        slot: r.slot,
        name: r.name,
        shortName: r.shortName,
        sub: r.sub,
        tier: r.tier,
        points: r.points,
      };
      this.picks.update((p) => [...p, pick]);
    }

    switch (r.result) {
      case 'hit':
        this.poppedSlot.set(r.slot ?? null);
        this.feedback.set({
          kind: 'hit',
          text: `${r.shortName ?? r.name}! ${r.tier} +${r.points}${r.sub ? ' (sub bonus included)' : ''}`,
        });
        break;
      case 'miss':
        this.feedback.set({ kind: 'miss', text: `${text.trim()} isn't in this lineup.` });
        break;
      case 'timeout':
        this.feedback.set({ kind: 'timeout', text: "Time's up. That guess is gone." });
        break;
      case 'duplicate':
        this.feedback.set({ kind: 'duplicate', text: `You already picked ${r.shortName ?? r.name}. Guess not used.` });
        break;
      case 'ambiguous':
        this.feedback.set({ kind: 'ambiguous', text: 'More than one player matches. Be more specific.' });
        break;
    }

    if (r.finished) {
      this.deadline.set(null);
      this.stopTicker();
      setTimeout(() => void this.finish(), this.revealDelayMs);
    } else if (consumed) {
      this.setDeadline(r.deadline ?? null, serverNow);
    }
  }

  private async finish(): Promise<void> {
    const id = this.playId();
    if (!id) return;
    try {
      const { body } = await this.api.reveal(id);
      this.reveal.set(body);
      this.score.set(body.totalScore);
      this.local.recordFinish(body.puzzleNumber);
      this.stopTicker();
      this.phase.set('revealed');
    } catch {
      this.phase.set('error');
      this.error.set("Couldn't load the reveal.");
    }
  }

  /** Convert the server's absolute deadline to the local clock using the server's Date header. */
  private setDeadline(deadlineIso: string | null, serverNow: number): void {
    if (!deadlineIso) {
      this.deadline.set(null);
      return;
    }
    const remaining = Date.parse(deadlineIso) - serverNow;
    const local = Date.now() + Math.min(remaining, GUESS_MS);
    this.deadline.set(local);
    this.now.set(Date.now());
  }

  private startTicker(): void {
    if (this.ticker) return;
    this.ticker = setInterval(() => this.tick(), 200);
  }

  private stopTicker(): void {
    if (this.ticker) clearInterval(this.ticker);
    this.ticker = null;
  }
}
