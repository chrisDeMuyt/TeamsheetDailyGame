import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ShareCardComponent } from './share-card.component';

const TEXT = 'Teamsheet #2 · 85 pts\n🟩🟩⚪🟩🟩\nhttps://teamsheet-daily.fly.dev';

describe('ShareCardComponent', () => {
  let share: ReturnType<typeof vi.fn>;
  let writeText: ReturnType<typeof vi.fn>;

  function setup(opts: { canShare: boolean; touch: boolean }) {
    share = vi.fn().mockResolvedValue(undefined);
    writeText = vi.fn().mockResolvedValue(undefined);
    vi.stubGlobal('navigator', {
      ...navigator,
      share: opts.canShare ? share : undefined,
      canShare: opts.canShare ? () => true : undefined,
      clipboard: { writeText },
    });
    vi.stubGlobal('matchMedia', (q: string) => ({ matches: opts.touch && q === '(pointer: coarse)' }));
    const fixture = TestBed.createComponent(ShareCardComponent);
    fixture.componentRef.setInput('text', TEXT);
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => vi.unstubAllGlobals());

  function shareButton(f: ReturnType<typeof setup>): HTMLButtonElement {
    return (f.nativeElement as HTMLElement).querySelector('.share')!;
  }

  it('Stats shows played, current streak, max streak and personal best (no card preview)', () => {
    const f = setup({ canShare: false, touch: false });
    f.componentRef.setInput('stats', { played: 6, currentStreak: 2, maxStreak: 4, bestScore: 205 });
    f.detectChanges();
    const el = f.nativeElement as HTMLElement;
    const tiles = [...el.querySelectorAll('dialog dl > div')]
      .map((d) => `${d.querySelector('dt')?.textContent?.trim()}: ${d.querySelector('dd')?.textContent?.trim()}`);
    expect(tiles).toEqual(['Played: 6', 'Current streak: 2', 'Max streak: 4', 'Personal best: 205']);
    expect(el.querySelector('dialog')?.textContent).not.toContain('Teamsheet #2');
  });

  it('phones open the native share sheet with the result text (iOS / Android)', async () => {
    const f = setup({ canShare: true, touch: true });
    await f.componentInstance.share();
    expect(share).toHaveBeenCalledWith({ text: TEXT });
    expect(writeText).not.toHaveBeenCalled();
    expect(shareButton(f).textContent?.trim()).toBe('Share result');
  });

  it('closing the share sheet is not an error and does not copy', async () => {
    const f = setup({ canShare: true, touch: true });
    share.mockRejectedValue(new DOMException('cancelled', 'AbortError'));
    await f.componentInstance.share();
    expect(writeText).not.toHaveBeenCalled();
  });

  it('falls back to copying if the share sheet fails', async () => {
    const f = setup({ canShare: true, touch: true });
    share.mockRejectedValue(new DOMException('nope', 'NotAllowedError'));
    await f.componentInstance.share();
    expect(writeText).toHaveBeenCalledWith(TEXT);
  });

  it('desktop copies to the clipboard even if the browser has a share API', async () => {
    const f = setup({ canShare: true, touch: false });
    await f.componentInstance.share();
    expect(share).not.toHaveBeenCalled();
    expect(writeText).toHaveBeenCalledWith(TEXT);
  });

  it('browsers without a share API copy to the clipboard', async () => {
    const f = setup({ canShare: false, touch: true });
    await f.componentInstance.share();
    expect(writeText).toHaveBeenCalledWith(TEXT);
  });
});
