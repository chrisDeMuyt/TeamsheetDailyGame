import { TestBed } from '@angular/core/testing';
import { LocalStore } from './local-store';

describe('LocalStore', () => {
  beforeEach(() => localStorage.clear());

  const fresh = () => {
    TestBed.resetTestingModule();
    return TestBed.inject(LocalStore);
  };

  it('creates one anonId and keeps it across reloads', () => {
    const id = fresh().anonId();
    expect(id).toMatch(/^[0-9a-f-]{36}$/);
    expect(fresh().anonId()).toBe(id);
  });

  it('grows the streak on consecutive puzzles, resets after a gap, ignores replays', () => {
    const s = fresh();
    s.recordFinish(5);
    expect(s.streak()).toBe(1);
    s.recordFinish(5); // same puzzle again (refresh on reveal)
    expect(s.streak()).toBe(1);
    s.recordFinish(6);
    expect(s.streak()).toBe(2);
    s.recordFinish(8); // skipped #7
    expect(s.streak()).toBe(1);
    expect(s.lastPlayed()).toBe(8);

    const reloaded = fresh();
    expect(reloaded.streak()).toBe(1);
    expect(reloaded.lastPlayed()).toBe(8);
  });
});
