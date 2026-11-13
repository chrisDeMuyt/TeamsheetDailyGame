import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ApiService } from './api/api.service';
import { App } from './app';
import { PUZZLE } from './testing/fixtures';

describe('App', () => {
  beforeEach(async () => {
    localStorage.clear();
    const now = Date.now();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [{
        provide: ApiService,
        useValue: {
          today: vi.fn().mockResolvedValue({ body: PUZZLE, serverNow: now }),
          createPlay: vi.fn().mockResolvedValue({
            body: { playId: 'p1', puzzleNumber: 1, guessesUsed: 0, guessesLeft: 3, finished: false, totalScore: 0,
                    deadline: new Date(now + 30_000).toISOString(), guesses: [] },
            serverNow: now,
          }),
        },
      }],
    }).compileComponents();
  });

  it('renders the in-game screen with positions only and an accessible guess form', async () => {
    const fixture = TestBed.createComponent(App);
    await vi.waitFor(async () => {
      await fixture.whenStable();
      expect(fixture.nativeElement.querySelector('app-pitch')).toBeTruthy();
    });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('h1')?.textContent).toContain('TEAMSHEET');
    expect(el.querySelector('h1')?.textContent).toContain('#1');
    expect(el.querySelectorAll('app-pitch app-spot')).toHaveLength(11);
    expect(el.querySelectorAll('app-subs-strip app-spot')).toHaveLength(3);
    expect(el.textContent).toContain('Champions League Final · 25 May 2005');
    expect(el.textContent).toContain('Guess 1 of 3');

    const input = el.querySelector<HTMLInputElement>('#guess-text')!;
    expect(el.querySelector(`label[for="${input.id}"]`)).toBeTruthy();
    const submit = el.querySelector<HTMLButtonElement>('button[type="submit"]')!;
    expect(submit.disabled).toBe(true); // nothing typed yet
  });
});
