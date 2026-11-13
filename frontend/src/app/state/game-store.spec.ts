import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ApiService, Timed } from '../api/api.service';
import { GuessResult, PlayView, PlayerOption } from '../api/models';
import { PUZZLE, reveal } from '../testing/fixtures';
import { GameStore } from './game-store';

const NOW = Date.now();
const t = <T>(body: T): Timed<T> => ({ body, serverNow: NOW });
const deadline = (s: number) => new Date(NOW + s * 1000).toISOString();

function play(over: Partial<PlayView> = {}): PlayView {
  return {
    playId: 'p1', puzzleNumber: 1, guessesUsed: 0, guessesLeft: 3, finished: false, totalScore: 0,
    deadline: deadline(30), guesses: [], ...over,
  };
}

function result(over: Partial<GuessResult>): GuessResult {
  return { result: 'miss', guessesLeft: 2, points: 0, totalScore: 0, finished: false, deadline: deadline(30), ...over };
}

const TRAORE: PlayerOption = { id: 4, name: 'Djimi Traoré', exact: true };
const JOHN_TERRY: PlayerOption = { id: 50, name: 'John Terry', exact: false };

describe('GameStore', () => {
  let api: Record<'today' | 'todayStatus' | 'createPlay' | 'guess' | 'guessPlayer' | 'suggest' | 'reveal', ReturnType<typeof vi.fn>>;
  let store: GameStore;

  /** Fresh device: Start screen, then Start. */
  async function startFresh(p: PlayView = play()) {
    api.todayStatus.mockResolvedValue(t({ puzzleNumber: 1, play: null }));
    api.createPlay.mockResolvedValue(t(p));
    store = TestBed.inject(GameStore);
    store.revealDelayMs = 0;
    await store.init();
    await store.start();
  }

  /** Returning device whose play already exists (refresh). */
  async function resume(p: PlayView) {
    api.todayStatus.mockResolvedValue(t({ puzzleNumber: 1, play: p }));
    store = TestBed.inject(GameStore);
    store.revealDelayMs = 0;
    await store.init();
  }

  beforeEach(() => {
    localStorage.clear();
    api = {
      today: vi.fn().mockResolvedValue(t(PUZZLE)),
      todayStatus: vi.fn(),
      createPlay: vi.fn(),
      guess: vi.fn(),
      guessPlayer: vi.fn(),
      suggest: vi.fn(),
      reveal: vi.fn().mockResolvedValue(t(reveal([4], 60))),
    };
    TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: api }] });
  });

  it('shows the Start screen without loading the match or starting the clock', async () => {
    api.todayStatus.mockResolvedValue(t({ puzzleNumber: 7, play: null }));
    store = TestBed.inject(GameStore);
    await store.init();
    expect(store.phase()).toBe('intro');
    expect(store.introNumber()).toBe(7);
    expect(api.today).not.toHaveBeenCalled();       // team stays hidden
    expect(api.createPlay).not.toHaveBeenCalled();  // no play = no clock
    expect(store.puzzle()).toBeNull();
  });

  it('Start loads the match, then creates the play (clock starts) with a persistent anonId', async () => {
    await startFresh();
    expect(store.phase()).toBe('playing');
    expect(api.today).toHaveBeenCalledBefore(api.createPlay);
    expect(api.createPlay).toHaveBeenCalledWith(localStorage.getItem('teamsheet.anonId'));
    expect(store.remainingMs()).toBeGreaterThan(25_000);
  });

  it('an exact full name is guessed straight away', async () => {
    await startFresh();
    api.suggest.mockResolvedValue(TRAORE);
    api.guessPlayer.mockResolvedValue(t(result({
      result: 'hit', guessIndex: 1, slot: 4, name: 'Djimi Traoré', shortName: 'Traoré', sub: false,
      tier: 'Proper Fan', points: 60, totalScore: 60,
    })));
    expect(await store.submitText('djimi traore')).toBe('hit');
    expect(api.guessPlayer).toHaveBeenCalledWith('p1', 4);
    expect(store.starters().find((s) => s.slot === 4)).toMatchObject({ state: 'hit', label: 'Traoré', points: 60 });
    expect(store.poppedSlot()).toBe(4);
    expect(store.suggestion()).toBeNull();
  });

  it('a non-exact name offers "Did you mean" and uses no guess until tapped', async () => {
    await startFresh();
    api.suggest.mockResolvedValue(JOHN_TERRY);
    expect(await store.submitText('John')).toBeNull();
    expect(store.suggestion()).toEqual(JOHN_TERRY);
    expect(api.guessPlayer).not.toHaveBeenCalled();
    expect(store.picks()).toHaveLength(0);

    api.guessPlayer.mockResolvedValue(t(result({ result: 'miss', guessIndex: 1 })));
    expect(await store.pickSuggestion()).toBe('miss');
    expect(api.guessPlayer).toHaveBeenCalledWith('p1', 50);
    expect(store.picks()[0]).toMatchObject({ text: 'John Terry', result: 'miss' });
    expect(store.suggestion()).toBeNull();
  });

  it('editing the text clears an old suggestion', async () => {
    await startFresh();
    api.suggest.mockResolvedValue(JOHN_TERRY);
    await store.submitText('John');
    store.clearSuggestion();
    expect(store.suggestion()).toBeNull();
    expect(await store.pickSuggestion()).toBeNull();
  });

  it('unknown names and too-short input use no guess', async () => {
    await startFresh();
    api.suggest.mockResolvedValue(null);
    expect(await store.submitText('Zzyzx')).toBeNull();
    expect(store.feedback()?.text).toMatch(/no player called/i);
    expect(await store.submitText('j')).toBeNull();
    expect(api.suggest).toHaveBeenCalledTimes(1);
    expect(store.picks()).toHaveLength(0);
  });

  it('duplicate does not use up a guess', async () => {
    await startFresh();
    api.guessPlayer.mockResolvedValueOnce(t(result({ result: 'duplicate', guessesLeft: 3, shortName: 'Gerrard' })));
    await store.submitPlayer({ id: 7, name: 'Steven Gerrard', exact: true });
    expect(store.picks()).toHaveLength(0);
    expect(store.feedback()?.kind).toBe('duplicate');
  });

  it('when the countdown runs out it sends one empty guess', async () => {
    await startFresh(play({ deadline: deadline(1) }));
    api.guess.mockResolvedValue(t(result({ result: 'timeout', guessIndex: 1, deadline: deadline(30) })));
    store.tick(Date.now() + 5_000);
    store.tick(Date.now() + 5_200);
    await Promise.resolve();
    expect(api.guess).toHaveBeenCalledTimes(1);
    expect(api.guess).toHaveBeenCalledWith('p1', '');
  });

  it('third guess finishes: shows the reveal and records the streak', async () => {
    await startFresh(play({ guessesUsed: 2, guessesLeft: 1 }));
    api.guessPlayer.mockResolvedValue(t(result({ result: 'miss', guessIndex: 3, guessesLeft: 0, finished: true })));
    await store.submitPlayer(JOHN_TERRY);
    await vi.waitFor(() => expect(store.phase()).toBe('revealed'));
    expect(localStorage.getItem('teamsheet.lastPlayed')).toBe('1');
    expect(localStorage.getItem('teamsheet.streak')).toBe('1');
  });

  it('refresh mid-game skips the Start screen and resumes the same play', async () => {
    await resume(play({
      guessesUsed: 1, guessesLeft: 2, totalScore: 75,
      guesses: [{ guessIndex: 1, text: 'smicer', result: 'hit', slot: 12, shortName: 'Šmicer', sub: true, tier: 'Proper Fan', points: 75 }],
    }));
    expect(store.phase()).toBe('playing');
    expect(api.createPlay).not.toHaveBeenCalled();
    expect(store.score()).toBe(75);
    expect(store.subs().find((s) => s.slot === 12)?.state).toBe('hit');
    expect(store.poppedSlot()).toBeNull();
  });

  it('a finished play goes straight to the reveal', async () => {
    await resume(play({ finished: true, guessesUsed: 3, guessesLeft: 0, deadline: null }));
    expect(store.phase()).toBe('revealed');
    expect(api.today).not.toHaveBeenCalled();
  });

  it('no puzzle today shows a friendly error', async () => {
    api.todayStatus.mockRejectedValue(new HttpErrorResponse({ status: 404 }));
    store = TestBed.inject(GameStore);
    await store.init();
    expect(store.phase()).toBe('error');
    expect(store.error()).toMatch(/no puzzle today/i);
  });
});
