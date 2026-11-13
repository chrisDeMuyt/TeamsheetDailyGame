import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ApiService, Timed } from '../api/api.service';
import { GuessResult, PlayView } from '../api/models';
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

describe('GameStore', () => {
  let api: { today: any; createPlay: any; guess: any; reveal: any };
  let store: GameStore;

  async function start(p: PlayView = play()) {
    api.createPlay.mockResolvedValue(t(p));
    store = TestBed.inject(GameStore);
    store.revealDelayMs = 0;
    await store.init();
  }

  beforeEach(() => {
    localStorage.clear();
    api = {
      today: vi.fn().mockResolvedValue(t(PUZZLE)),
      createPlay: vi.fn(),
      guess: vi.fn(),
      reveal: vi.fn().mockResolvedValue(t(reveal([4], 60))),
    };
    TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: api }] });
  });

  it('loads today, creates a play with a persistent anonId and starts playing', async () => {
    await start();
    expect(store.phase()).toBe('playing');
    expect(store.guessNumber()).toBe(1);
    expect(store.remainingMs()).toBeGreaterThan(25_000);
    expect(api.createPlay).toHaveBeenCalledWith(localStorage.getItem('teamsheet.anonId'));
  });

  it('hit: records the pick, fills its slot and pops it', async () => {
    await start();
    api.guess.mockResolvedValue(t(result({
      result: 'hit', guessIndex: 1, slot: 4, name: 'Djimi Traoré', shortName: 'Traoré', sub: false,
      tier: 'Proper Fan', points: 60, totalScore: 60,
    })));
    expect(await store.submit('traore')).toBe('hit');
    expect(store.picks()).toHaveLength(1);
    expect(store.score()).toBe(60);
    expect(store.poppedSlot()).toBe(4);
    expect(store.starters().find((s) => s.slot === 4)).toMatchObject({ state: 'hit', label: 'Traoré', points: 60 });
    expect(store.guessNumber()).toBe(2);
  });

  it('miss and timeout use up a guess', async () => {
    await start();
    api.guess.mockResolvedValueOnce(t(result({ result: 'miss', guessIndex: 1 })));
    await store.submit('Diao');
    api.guess.mockResolvedValueOnce(t(result({ result: 'timeout', guessIndex: 2, guessesLeft: 1 })));
    await store.submit('');
    expect(store.picks().map((p) => p.result)).toEqual(['miss', 'timeout']);
    expect(store.feedback()?.kind).toBe('timeout');
  });

  it('ambiguous and duplicate do not use up a guess', async () => {
    await start();
    api.guess.mockResolvedValueOnce(t(result({ result: 'ambiguous', guessesLeft: 3 })));
    expect(await store.submit('Dji')).toBe('ambiguous');
    expect(store.feedback()?.text).toMatch(/be more specific/i);
    api.guess.mockResolvedValueOnce(t(result({ result: 'duplicate', guessesLeft: 3, shortName: 'Gerrard' })));
    await store.submit('Gerrard');
    expect(store.picks()).toHaveLength(0);
    expect(store.guessNumber()).toBe(1);
  });

  it('when the countdown runs out it sends one empty guess', async () => {
    await start(play({ deadline: deadline(1) }));
    api.guess.mockResolvedValue(t(result({ result: 'timeout', guessIndex: 1, deadline: deadline(30) })));
    store.tick(Date.now() + 5_000);
    store.tick(Date.now() + 5_200);
    await Promise.resolve();
    expect(api.guess).toHaveBeenCalledTimes(1);
    expect(api.guess).toHaveBeenCalledWith('p1', '');
  });

  it('third guess finishes: shows the reveal and records the streak', async () => {
    await start(play({ guessesUsed: 2, guessesLeft: 1 }));
    api.guess.mockResolvedValue(t(result({ result: 'miss', guessIndex: 3, guessesLeft: 0, finished: true })));
    await store.submit('Henry');
    await new Promise((r) => setTimeout(r, 0));
    await vi.waitFor(() => expect(store.phase()).toBe('revealed'));
    expect(store.starters().some((s) => s.state === 'picked')).toBe(true);
    expect(localStorage.getItem('teamsheet.lastPlayed')).toBe('1');
    expect(localStorage.getItem('teamsheet.streak')).toBe('1');
  });

  it('refresh mid-game resumes the same play with its picks', async () => {
    await start(play({
      guessesUsed: 1, guessesLeft: 2, totalScore: 75,
      guesses: [{ guessIndex: 1, text: 'smicer', result: 'hit', slot: 12, shortName: 'Šmicer', sub: true, tier: 'Proper Fan', points: 75 }],
    }));
    expect(store.phase()).toBe('playing');
    expect(store.score()).toBe(75);
    expect(store.guessNumber()).toBe(2);
    expect(store.subs().find((s) => s.slot === 12)?.state).toBe('hit');
    expect(store.poppedSlot()).toBeNull(); // no animation on resume
  });

  it('a finished play goes straight to the reveal', async () => {
    await start(play({ finished: true, guessesUsed: 3, guessesLeft: 0, deadline: null }));
    expect(store.phase()).toBe('revealed');
    expect(api.reveal).toHaveBeenCalledWith('p1');
  });

  it('no puzzle today shows a friendly error', async () => {
    api.today.mockRejectedValue(new HttpErrorResponse({ status: 404 }));
    store = TestBed.inject(GameStore);
    await store.init();
    expect(store.phase()).toBe('error');
    expect(store.error()).toMatch(/no puzzle today/i);
  });
});
