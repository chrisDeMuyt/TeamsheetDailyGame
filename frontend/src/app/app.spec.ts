import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ApiService } from './api/api.service';
import { App } from './app';
import { PUZZLE } from './testing/fixtures';

describe('App', () => {
  let api: Record<'todayStatus' | 'today' | 'createPlay' | 'suggest', ReturnType<typeof vi.fn>>;

  beforeEach(async () => {
    localStorage.clear();
    const now = Date.now();
    api = {
      todayStatus: vi.fn().mockResolvedValue({ body: { puzzleNumber: 1, play: null }, serverNow: now }),
      today: vi.fn().mockResolvedValue({ body: PUZZLE, serverNow: now }),
      createPlay: vi.fn().mockResolvedValue({
        body: { playId: 'p1', puzzleNumber: 1, guessesUsed: 0, guessesLeft: 3, finished: false, totalScore: 0,
                deadline: new Date(now + 30_000).toISOString(), guesses: [] },
        serverNow: now,
      }),
      suggest: vi.fn().mockResolvedValue({ id: 50, name: 'John Terry', exact: false }),
    };
    await TestBed.configureTestingModule({ imports: [App], providers: [{ provide: ApiService, useValue: api }] })
      .compileComponents();
  });

  async function stable(fixture: ReturnType<typeof TestBed.createComponent<App>>, check: () => void) {
    await vi.waitFor(async () => {
      await fixture.whenStable();
      fixture.detectChanges();
      check();
    });
  }

  it('shows the Start screen first, with no team or pitch on the page', async () => {
    const fixture = TestBed.createComponent(App);
    const el = fixture.nativeElement as HTMLElement;
    await stable(fixture, () => expect(el.querySelector('app-intro')).toBeTruthy());
    expect(el.querySelector('h1')?.textContent).toContain('TEAMSHEET');
    expect(el.textContent).toContain('#1');
    expect(el.textContent).not.toContain('Liverpool');
    expect(el.querySelector('app-pitch')).toBeNull();
    expect(api.createPlay).not.toHaveBeenCalled();
    const start = el.querySelector<HTMLButtonElement>('app-intro button')!;
    expect(start.textContent?.trim()).toBe('Start');
  });

  it('Start reveals the lineup (positions only) and the guess form', async () => {
    const fixture = TestBed.createComponent(App);
    const el = fixture.nativeElement as HTMLElement;
    await stable(fixture, () => expect(el.querySelector('app-intro button')).toBeTruthy());
    el.querySelector<HTMLButtonElement>('app-intro button')!.click();
    await stable(fixture, () => expect(el.querySelector('app-pitch')).toBeTruthy());

    expect(el.querySelectorAll('app-pitch app-spot')).toHaveLength(11);
    expect(el.querySelectorAll('app-subs-strip app-spot')).toHaveLength(3);
    expect(el.textContent).toContain('Champions League Final · 25 May 2005');
    expect(el.textContent).toContain('Guess 1 of 3');
    const input = el.querySelector<HTMLInputElement>('#guess-text')!;
    expect(el.querySelector(`label[for="${input.id}"]`)).toBeTruthy();
  });

  it('a non-exact name shows one "Did you mean" button', async () => {
    const fixture = TestBed.createComponent(App);
    const el = fixture.nativeElement as HTMLElement;
    await stable(fixture, () => expect(el.querySelector('app-intro button')).toBeTruthy());
    el.querySelector<HTMLButtonElement>('app-intro button')!.click();
    await stable(fixture, () => expect(el.querySelector('#guess-text')).toBeTruthy());

    const input = el.querySelector<HTMLInputElement>('#guess-text')!;
    input.value = 'John';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    input.form!.requestSubmit();
    await stable(fixture, () => expect(el.querySelector('.suggest-btn')).toBeTruthy());

    expect(el.querySelectorAll('.suggest-btn')).toHaveLength(1);
    expect(el.querySelector('.suggest')?.textContent).toContain('Did you mean');
    expect(el.querySelector('.suggest-btn')?.textContent?.trim()).toBe('John Terry?');
    expect(api.suggest).toHaveBeenCalledWith('John');
  });
});
